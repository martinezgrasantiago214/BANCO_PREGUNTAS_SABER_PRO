package co.unicauca.saberpro.review.infrastructure.adapter.in.web;

import co.unicauca.saberpro.review.application.port.in.AssignReviewersUseCase;
import co.unicauca.saberpro.review.application.port.in.AssignReviewersUseCase.AssignReviewersCommand;
import co.unicauca.saberpro.review.application.port.in.EvaluateQuestionUseCase;
import co.unicauca.saberpro.review.application.port.in.EvaluateQuestionUseCase.EvaluateQuestionCommand;
import co.unicauca.saberpro.review.application.port.in.ReviewQueries;
import co.unicauca.saberpro.review.domain.model.Evaluation;
import co.unicauca.saberpro.review.domain.model.QuestionReview;
import co.unicauca.saberpro.review.domain.model.ReviewRound;
import co.unicauca.saberpro.review.domain.model.ReviewerAssignment;
import co.unicauca.saberpro.review.infrastructure.adapter.in.web.ReviewDtos.AssignReviewersInput;
import co.unicauca.saberpro.review.infrastructure.adapter.in.web.ReviewDtos.AssignmentResponse;
import co.unicauca.saberpro.review.infrastructure.adapter.in.web.ReviewDtos.EvaluationInput;
import co.unicauca.saberpro.review.infrastructure.adapter.in.web.ReviewDtos.EvaluationResponse;
import co.unicauca.saberpro.review.infrastructure.adapter.in.web.ReviewDtos.PendingQuestionResponse;
import co.unicauca.saberpro.review.infrastructure.adapter.in.web.ReviewDtos.ReviewHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada (REST) de la revision por pares: HU04, HU05 y HU06.
 *
 * Solo traduce HTTP <-> casos de uso; las reglas las decide el dominio.
 *
 * Mientras no llega la seguridad con Keycloak (issue #49), la identidad viaja
 * en la peticion como en el segundo corte: quien asigna en {@code assignedBy},
 * quien evalua en {@code reviewerLogin} y quien consulta el historial en el
 * parametro opcional {@code requester} (sin el, la consulta no se restringe).
 * En #49 todo esto pasa a salir del token JWT.
 */
@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Revision por pares")
public class ReviewController {

    private final AssignReviewersUseCase assignReviewers;
    private final EvaluateQuestionUseCase evaluateQuestion;
    private final ReviewQueries queries;

    public ReviewController(AssignReviewersUseCase assignReviewers, EvaluateQuestionUseCase evaluateQuestion,
                            ReviewQueries queries) {
        this.assignReviewers = assignReviewers;
        this.evaluateQuestion = evaluateQuestion;
        this.queries = queries;
    }

    @GetMapping("/pending-questions")
    @Operation(summary = "HU04: preguntas que esperan veredicto y que el administrador puede asignar [PSP-30]")
    public List<PendingQuestionResponse> pendingQuestions() {
        return queries.pendingQuestions().stream().map(PendingQuestionResponse::from).toList();
    }

    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "HU04: asigna entre 1 y 3 revisores a la ronda actual [PSP-31..34]")
    public List<AssignmentResponse> assign(@RequestBody AssignReviewersInput input) {
        String admin = input.assignedBy();
        List<ReviewerAssignment> created = assignReviewers.assign(
                new AssignReviewersCommand(input.questionId(), input.reviewers(), admin));
        QuestionReview review = queries.history(input.questionId(), admin, true);
        ReviewRound round = review.currentRound();
        return created.stream().map(a -> AssignmentResponse.from(review.questionId(), round, a, true)).toList();
    }

    @GetMapping("/assignments")
    @Operation(summary = "HU05: preguntas asignadas a un revisor [PSP-45]")
    public List<AssignmentResponse> assignedTo(@RequestParam String reviewer) {
        return queries.assignedTo(reviewer.trim()).stream().map(AssignmentResponse::from).toList();
    }

    @PostMapping("/evaluations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "HU05: el revisor aprueba o rechaza con observaciones [PSP-47..49]")
    public EvaluationResponse evaluate(@RequestBody EvaluationInput input) {
        String reviewer = input.reviewerLogin();
        Evaluation evaluation = evaluateQuestion.evaluate(new EvaluateQuestionCommand(input.questionId(), reviewer,
                input.verdict(), input.observations()));
        QuestionReview review = queries.history(input.questionId(), reviewer, false);
        return EvaluationResponse.from(review.questionId(), review.currentRound(), evaluation);
    }

    @GetMapping("/evaluations")
    @Operation(summary = "HU05/HU06: evaluaciones y observaciones de todas las rondas de una pregunta [PSP-47]")
    public List<EvaluationResponse> evaluations(@RequestParam String questionId,
                                                @RequestParam(required = false) String requester) {
        return ReviewDtos.evaluationsOf(reviewFor(questionId, requester));
    }

    @GetMapping("/history/{questionId}")
    @Operation(summary = "HU06: historial de revisiones (rondas, revisores, veredictos y observaciones)")
    public ReviewHistoryResponse history(@PathVariable String questionId,
                                         @RequestParam(required = false) String requester) {
        return ReviewHistoryResponse.from(reviewFor(questionId, requester));
    }

    /** Sin {@code requester} no se restringe la consulta (comportamiento del segundo corte, hasta #49). */
    private QuestionReview reviewFor(String questionId, String requester) {
        boolean anonymous = requester == null || requester.isBlank();
        return queries.history(questionId, anonymous ? null : requester.trim(), anonymous);
    }
}
