package co.unicauca.saberpro.events;

import java.time.Instant;
import java.util.List;

/**
 * Publicado por review-service cuando la politica de decision determina el
 * resultado final de la revision. decision = "APROBADA" | "RECHAZADA".
 */
public record ReviewCompletedEvent(
        String eventId,
        Instant occurredAt,
        String questionId,
        String authorLogin,
        String directQuestion,
        String decision,
        List<String> reviewerLogins) implements IntegrationEvent {
}
