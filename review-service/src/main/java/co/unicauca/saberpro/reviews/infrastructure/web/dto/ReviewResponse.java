package co.unicauca.saberpro.reviews.infrastructure.web.dto;

import co.unicauca.saberpro.reviews.domain.model.QuestionReview;

import java.time.Instant;
import java.util.List;

public record ReviewResponse(String questionId, String authorLogin, String statement, String competence,
                             String topic, String status, String statusLabel, int round, Instant submittedAt,
                             String assignedBy, Instant assignedAt, String finalDecision,
                             List<AssignmentResponse> assignments, List<ObservationResponse> observations) {

    public static ReviewResponse from(QuestionReview r) {
        return new ReviewResponse(r.getQuestionId(), r.getAuthorLogin(), r.getStatement(), r.getCompetence(),
                r.getTopic(), r.getStatus().name(), r.getStatus().getLabel(), r.getRound(), r.getSubmittedAt(),
                r.getAssignedBy(), r.getAssignedAt(),
                r.getFinalDecision() == null ? null : r.getFinalDecision().name(),
                r.getAssignments().stream().map(AssignmentResponse::from).toList(),
                r.getObservations().stream().map(ObservationResponse::from).toList());
    }
}
