package co.unicauca.saberpro.reviews.infrastructure.web.dto;

import co.unicauca.saberpro.reviews.domain.model.ReviewerAssignment;

import java.time.Instant;
import java.util.List;

public record AssignmentResponse(String reviewerLogin, String status, String verdict, double averageScore,
                                 List<ScoreResponse> scores, Instant assignedAt, Instant completedAt) {

    public record ScoreResponse(String criterion, String criterionLabel, int score, String comment) {
    }

    public static AssignmentResponse from(ReviewerAssignment a) {
        return new AssignmentResponse(a.getReviewerLogin(), a.getStatus().name(),
                a.getVerdict() == null ? null : a.getVerdict().name(), a.averageScore(),
                a.getScores().stream().map(s -> new ScoreResponse(s.criterion().name(), s.criterion().getLabel(),
                        s.score(), s.comment())).toList(),
                a.getAssignedAt(), a.getCompletedAt());
    }
}
