package co.unicauca.saberpro.client.api.dto;

import java.time.Instant;
import java.util.List;

public record ReviewDto(String questionId, String authorLogin, String statement, String competence, String topic,
                        String status, String statusLabel, int round, Instant submittedAt, String assignedBy,
                        Instant assignedAt, String finalDecision, List<Assignment> assignments,
                        List<ObservationDto> observations) {

    public record Assignment(String reviewerLogin, String status, String verdict, double averageScore,
                             List<Score> scores, Instant assignedAt, Instant completedAt) {
    }

    public record Score(String criterion, String criterionLabel, int score, String comment) {
    }

    public Assignment assignmentOf(String login) {
        return assignments == null ? null
                : assignments.stream().filter(a -> a.reviewerLogin().equals(login)).findFirst().orElse(null);
    }
}
