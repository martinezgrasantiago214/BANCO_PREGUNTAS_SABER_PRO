package co.unicauca.saberpro.review.infrastructure.adapter.in.web;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.review.application.port.in.ReviewQueries.AssignedReview;
import co.unicauca.saberpro.review.domain.model.Evaluation;
import co.unicauca.saberpro.review.domain.model.QuestionReview;
import co.unicauca.saberpro.review.domain.model.ReviewRound;
import co.unicauca.saberpro.review.domain.model.ReviewerAssignment;

import java.time.Instant;
import java.util.List;

/**
 * DTOs del API del review-service: el JSON no expone el agregado, asi el
 * modelo de dominio puede evolucionar sin romper al frontend.
 */
public final class ReviewDtos {

    private ReviewDtos() {
    }

    /**
     * Cuerpo de POST /api/reviews/assignments (HU04). assignedBy es el
     * administrador que asigna (en #49 pasa a salir del token).
     */
    public record AssignReviewersInput(String questionId, List<String> reviewers, String assignedBy) {
    }

    /** Cuerpo de POST /api/reviews/evaluations (HU05). reviewerLogin es el revisor (en #49 pasa a salir del token). */
    public record EvaluationInput(String questionId, String reviewerLogin, EvaluationVerdict verdict,
                                  String observations) {
    }

    /** Pregunta que espera veredicto con los revisores que ya tiene en la ronda actual (HU04). */
    public record PendingQuestionResponse(String questionId, String authorLogin, String directQuestion,
                                          String submittedAt, List<String> assignedReviewers, int round,
                                          int questionVersion, String status, String statusLabel) {

        public static PendingQuestionResponse from(QuestionReview review) {
            ReviewRound round = review.currentRound();
            return new PendingQuestionResponse(review.questionId(), review.authorLogin(), round.directQuestion(),
                    text(round.submittedAt()), round.reviewerLogins(), round.number(), round.questionVersion(),
                    round.status().name(), round.status().getLabel());
        }
    }

    /** Asignacion de un revisor; active=true si aun puede evaluarla (ronda vigente sin veredicto). */
    public record AssignmentResponse(String questionId, String reviewerLogin, String assignedBy, String assignedAt,
                                     int round, int questionVersion, String roundStatus, boolean active) {

        public static AssignmentResponse from(String questionId, ReviewRound round, ReviewerAssignment a,
                                              boolean currentRound) {
            return new AssignmentResponse(questionId, a.reviewerLogin(), a.assignedBy(), text(a.assignedAt()),
                    round.number(), round.questionVersion(), round.status().name(),
                    currentRound && round.status().acceptsEvaluation());
        }

        public static AssignmentResponse from(AssignedReview a) {
            return new AssignmentResponse(a.questionId(), a.assignment().reviewerLogin(), a.assignment().assignedBy(),
                    text(a.assignment().assignedAt()), a.round(), a.questionVersion(), a.roundStatus().name(),
                    a.awaitingMyEvaluation());
        }
    }

    public record EvaluationResponse(String id, String questionId, String reviewerLogin, EvaluationVerdict verdict,
                                     String observations, String evaluatedAt, int round, int questionVersion) {

        public static EvaluationResponse from(String questionId, ReviewRound round, Evaluation e) {
            return new EvaluationResponse(e.id(), questionId, e.reviewerLogin(), e.verdict(), e.observations(),
                    text(e.evaluatedAt()), round.number(), round.questionVersion());
        }
    }

    /** HU06: una ronda del historial, con sus revisores y, si ya se decidio, la evaluacion. */
    public record RoundResponse(int round, int questionVersion, String directQuestion, String submittedAt,
                                String status, String statusLabel, List<AssignmentResponse> reviewers,
                                EvaluationResponse evaluation) {

        static RoundResponse from(QuestionReview review, ReviewRound round) {
            boolean current = round == review.currentRound();
            return new RoundResponse(round.number(), round.questionVersion(), round.directQuestion(),
                    text(round.submittedAt()), round.status().name(), round.status().getLabel(),
                    round.assignments().stream()
                            .map(a -> AssignmentResponse.from(review.questionId(), round, a, current)).toList(),
                    round.evaluation().map(e -> EvaluationResponse.from(review.questionId(), round, e)).orElse(null));
        }
    }

    /** HU06: historial completo de revisiones de una pregunta, de la primera ronda a la actual. */
    public record ReviewHistoryResponse(String questionId, String authorLogin, int currentRound,
                                        int currentQuestionVersion, String currentStatus, String currentStatusLabel,
                                        List<RoundResponse> rounds) {

        public static ReviewHistoryResponse from(QuestionReview review) {
            ReviewRound current = review.currentRound();
            return new ReviewHistoryResponse(review.questionId(), review.authorLogin(), current.number(),
                    current.questionVersion(), current.status().name(), current.status().getLabel(),
                    review.rounds().stream().map(r -> RoundResponse.from(review, r)).toList());
        }
    }

    /** Todas las evaluaciones del historial (compatibilidad con el endpoint del segundo corte). */
    public static List<EvaluationResponse> evaluationsOf(QuestionReview review) {
        return review.rounds().stream()
                .flatMap(r -> r.evaluation().stream().map(e -> EvaluationResponse.from(review.questionId(), r, e)))
                .toList();
    }

    private static String text(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
