package co.unicauca.saberpro.events;

import java.time.Instant;

/** Publicado por question-service cuando un autor envia su pregunta a revision (HU02). */
public record QuestionSubmittedForReviewEvent(
        String eventId,
        Instant occurredAt,
        String questionId,
        String authorLogin,
        String directQuestion,
        String competence,
        String topic,
        String subtopic,
        String difficulty) implements IntegrationEvent {
}
