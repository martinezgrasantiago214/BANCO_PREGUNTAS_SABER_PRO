package co.unicauca.saberpro.events;

import java.time.Instant;

/** Publicado por review-service cuando un revisor registra su evaluacion (HU05). */
public record ReviewEvaluationSubmittedEvent(
        String eventId,
        Instant occurredAt,
        String questionId,
        String authorLogin,
        String directQuestion,
        String reviewerLogin,
        String verdict,
        double averageScore) implements IntegrationEvent {
}
