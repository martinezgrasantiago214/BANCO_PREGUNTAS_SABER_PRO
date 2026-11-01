package co.unicauca.saberpro.review.web;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.review.domain.Evaluation;
import co.unicauca.saberpro.review.domain.PendingQuestion;
import co.unicauca.saberpro.review.domain.ReviewAssignment;

import java.util.List;

/** DTOs del API del review-service. */
public final class ReviewDtos {

    private ReviewDtos() {
    }

    /** Cuerpo de POST /api/reviews/assignments (HU04). */
    public record AssignReviewersInput(String questionId, List<String> reviewers, String assignedBy) {
    }

    /** Cuerpo de POST /api/reviews/evaluations (HU05). */
    public record EvaluationInput(String questionId, String reviewerLogin, EvaluationVerdict verdict,
                                  String observations) {
    }

    public record PendingQuestionResponse(String questionId, String authorLogin, String directQuestion,
                                          String submittedAt) {

        public static PendingQuestionResponse from(PendingQuestion q) {
            return new PendingQuestionResponse(q.questionId(), q.authorLogin(), q.directQuestion(),
                    q.submittedAt() != null ? q.submittedAt().toString() : null);
        }
    }

    public record AssignmentResponse(String questionId, String reviewerLogin, String assignedBy, String assignedAt) {

        public static AssignmentResponse from(ReviewAssignment a) {
            return new AssignmentResponse(a.getQuestionId(), a.getReviewerLogin(), a.getAssignedBy(),
                    a.getAssignedAt().toString());
        }
    }

    public record EvaluationResponse(String id, String questionId, String reviewerLogin, EvaluationVerdict verdict,
                                     String observations, String evaluatedAt) {

        public static EvaluationResponse from(Evaluation e) {
            return new EvaluationResponse(e.getId(), e.getQuestionId(), e.getReviewerLogin(), e.getVerdict(),
                    e.getObservations(), e.getEvaluatedAt() != null ? e.getEvaluatedAt().toString() : null);
        }
    }
}
