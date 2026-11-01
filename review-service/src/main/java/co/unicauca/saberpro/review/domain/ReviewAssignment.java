package co.unicauca.saberpro.review.domain;

import java.time.Instant;
import java.util.Objects;

/** HU04: un revisor asignado por el administrador a una pregunta Pendiente de revision. */
public class ReviewAssignment {

    private final String questionId;
    private final String reviewerLogin;
    private final String assignedBy;
    private final Instant assignedAt;

    public ReviewAssignment(String questionId, String reviewerLogin, String assignedBy, Instant assignedAt) {
        this.questionId = Objects.requireNonNull(questionId, "questionId");
        this.reviewerLogin = Objects.requireNonNull(reviewerLogin, "reviewerLogin");
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt != null ? assignedAt : Instant.now();
    }

    public String getQuestionId() { return questionId; }
    public String getReviewerLogin() { return reviewerLogin; }
    public String getAssignedBy() { return assignedBy; }
    public Instant getAssignedAt() { return assignedAt; }

    public boolean isFor(String questionId, String reviewerLogin) {
        return this.questionId.equals(questionId) && this.reviewerLogin.equals(reviewerLogin);
    }
}
