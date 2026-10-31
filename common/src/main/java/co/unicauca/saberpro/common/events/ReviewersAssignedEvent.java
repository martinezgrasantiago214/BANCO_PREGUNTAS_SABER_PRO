package co.unicauca.saberpro.common.events;

import java.time.Instant;
import java.util.List;

/**
 * HU04: el administrador asigno entre 1 y 3 revisores a una pregunta. Lo
 * publica el review-service y lo consume el notification-service para enviar
 * el correo a cada revisor.
 */
public record ReviewersAssignedEvent(String questionId, String directQuestion, List<String> reviewerLogins,
                                     String assignedBy, Instant occurredAt) implements DomainEvent {

    @Override
    public String routingKey() {
        return EventTopics.REVIEWERS_ASSIGNED;
    }
}
