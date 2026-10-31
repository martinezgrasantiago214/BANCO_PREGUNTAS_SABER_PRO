package co.unicauca.saberpro.events;

import java.time.Instant;
import java.util.List;

/** Publicado por review-service cuando el administrador asigna de 1 a 3 revisores (HU04). */
public record ReviewersAssignedEvent(
        String eventId,
        Instant occurredAt,
        String questionId,
        String authorLogin,
        String directQuestion,
        List<String> reviewerLogins,
        String assignedBy) implements IntegrationEvent {
}
