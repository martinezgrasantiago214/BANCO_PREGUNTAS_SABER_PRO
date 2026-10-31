package co.unicauca.saberpro.events;

import java.time.Instant;

/** Publicado por review-service cuando un revisor deja una observacion para el autor (HU05). */
public record ObservationAddedEvent(
        String eventId,
        Instant occurredAt,
        String questionId,
        String authorLogin,
        String directQuestion,
        String reviewerLogin,
        String text) implements IntegrationEvent {
}
