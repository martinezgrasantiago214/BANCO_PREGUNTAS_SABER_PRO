package co.unicauca.saberpro.reviews.infrastructure.web;

import co.unicauca.saberpro.reviews.application.ReviewService;
import co.unicauca.saberpro.reviews.domain.model.AssignmentStatus;
import co.unicauca.saberpro.reviews.domain.model.CriterionScore;
import co.unicauca.saberpro.reviews.domain.model.ReviewCriterion;
import co.unicauca.saberpro.reviews.infrastructure.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Revisión por pares", description = "HU04 asignar revisores, HU05 evaluar preguntas")
public class ReviewController {

    static final String USER_HEADER = "X-User-Login";
    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @GetMapping("/pending-assignment")
    @Operation(summary = "HU04 - Preguntas Pendientes de revisión que esperan revisores")
    public PageResponse<ReviewResponse> pending(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return PageResponse.from(service.listPendingAssignment(page, size).map(ReviewResponse::from));
    }

    @PostMapping("/{questionId}/reviewers")
    @Operation(summary = "HU04 - Asigna de 1 a 3 revisores (notifica por correo vía evento)")
    public ReviewResponse assign(@RequestHeader(USER_HEADER) String admin, @PathVariable String questionId,
                                 @RequestBody AssignReviewersRequest body) {
        return ReviewResponse.from(service.assignReviewers(questionId, body.reviewerLogins(), admin));
    }

    @GetMapping("/assigned")
    @Operation(summary = "HU05 - Preguntas asignadas al revisor autenticado")
    public PageResponse<ReviewResponse> assigned(
            @RequestHeader(USER_HEADER) String reviewer,
            @Parameter(description = "ASIGNADA (pendientes), COMPLETADA o vacío para todas")
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return PageResponse.from(service.listAssignedTo(reviewer, status, page, size).map(ReviewResponse::from));
    }

    @GetMapping("/{questionId}")
    @Operation(summary = "Detalle del proceso de revisión de una pregunta")
    public ReviewResponse get(@PathVariable String questionId) {
        return ReviewResponse.from(service.get(questionId));
    }

    @PostMapping("/{questionId}/evaluations")
    @Operation(summary = "HU05 - El revisor califica la rúbrica y deja su concepto Aprobada/Rechazada")
    public ReviewResponse evaluate(@RequestHeader(USER_HEADER) String reviewer, @PathVariable String questionId,
                                   @RequestBody EvaluationRequest body) {
        return ReviewResponse.from(service.submitEvaluation(questionId, reviewer, body.toCommand()));
    }

    @PostMapping("/{questionId}/observations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "HU05 - El revisor deja una observación para el autor")
    public ObservationResponse observe(@RequestHeader(USER_HEADER) String reviewer, @PathVariable String questionId,
                                       @RequestBody ObservationRequest body) {
        return ObservationResponse.from(service.addObservation(questionId, reviewer, body.text()));
    }

    @GetMapping("/{questionId}/observations")
    @Operation(summary = "HU05 - El autor (o un revisor/administrador) lee las observaciones")
    public List<ObservationResponse> observations(@RequestHeader(USER_HEADER) String user,
                                                  @PathVariable String questionId) {
        return service.observationsFor(questionId, user).stream().map(ObservationResponse::from).toList();
    }

    @GetMapping("/catalog/criteria")
    @Operation(summary = "Rúbrica de evaluación del revisor")
    public List<CriterionInfo> criteria() {
        return Arrays.stream(ReviewCriterion.values())
                .map(c -> new CriterionInfo(c.name(), c.getLabel(), CriterionScore.MIN_SCORE, CriterionScore.MAX_SCORE))
                .toList();
    }

    @GetMapping("/catalog/decision-policy")
    @Operation(summary = "Política (Strategy) con la que se combinan los conceptos de los revisores")
    public String decisionPolicy() {
        return service.getDecisionPolicy().name();
    }
}
