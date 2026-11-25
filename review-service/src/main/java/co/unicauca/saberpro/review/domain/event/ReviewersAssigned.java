package co.unicauca.saberpro.review.domain.event;

import java.time.Instant;
import java.util.List;

/** HU04: el administrador asigno revisores en una ronda; hay que avisarles. */
public record ReviewersAssigned(String questionId, int round, String directQuestion, List<String> reviewerLogins,
                                String assignedBy, Instant occurredAt) implements ReviewDomainEvent {

    public ReviewersAssigned {
        reviewerLogins = List.copyOf(reviewerLogins);
    }
}
