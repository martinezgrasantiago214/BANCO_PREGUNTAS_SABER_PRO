package co.unicauca.saberpro.review.web;

import co.unicauca.saberpro.review.application.ReviewApplicationService;
import co.unicauca.saberpro.review.web.ReviewDtos.AssignReviewersInput;
import co.unicauca.saberpro.review.web.ReviewDtos.AssignmentResponse;
import co.unicauca.saberpro.review.web.ReviewDtos.EvaluationInput;
import co.unicauca.saberpro.review.web.ReviewDtos.EvaluationResponse;
import co.unicauca.saberpro.review.web.ReviewDtos.PendingQuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API REST de revision por pares. Los casos de uso pendientes responden 501 indicando su issue de Jira. */
@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Revision por pares")
public class ReviewController {

    private final ReviewApplicationService service;

    public ReviewController(ReviewApplicationService service) {
        this.service = service;
    }

    @GetMapping("/pending-questions")
    @Operation(summary = "HU04: preguntas Pendientes de revision que el administrador puede asignar [PSP-30]")
    public List<PendingQuestionResponse> pendingQuestions() {
        return service.listPendingQuestions().stream().map(PendingQuestionResponse::from).toList();
    }

    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "HU04: asigna entre 1 y 3 revisores a una pregunta [PSP-31..34]")
    public List<AssignmentResponse> assign(@RequestBody AssignReviewersInput input) {
        return service.assignReviewers(input.questionId(), input.reviewers(), input.assignedBy()).stream()
                .map(AssignmentResponse::from).toList();
    }

    @GetMapping("/assignments")
    @Operation(summary = "HU05: preguntas asignadas a un revisor [PSP-45]")
    public List<AssignmentResponse> assignedTo(@RequestParam String reviewer) {
        return service.listAssignedTo(reviewer).stream().map(AssignmentResponse::from).toList();
    }

    @PostMapping("/evaluations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "HU05: registra la evaluacion (Aprobada/Rechazada) y observaciones [PSP-47..49]")
    public EvaluationResponse evaluate(@RequestBody EvaluationInput input) {
        return EvaluationResponse.from(service.evaluate(input.questionId(), input.reviewerLogin(),
                input.verdict(), input.observations()));
    }

    @GetMapping("/evaluations")
    @Operation(summary = "HU05: observaciones de una pregunta, para que las lea el autor [PSP-47]")
    public List<EvaluationResponse> evaluations(@RequestParam String questionId) {
        return service.listEvaluations(questionId).stream().map(EvaluationResponse::from).toList();
    }
}
