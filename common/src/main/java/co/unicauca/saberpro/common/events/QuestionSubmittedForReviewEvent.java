package co.unicauca.saberpro.common.events;

import java.time.Instant;

/**
 * HU02: el autor paso una pregunta de Borrador a Pendiente de revision. Lo
 * publica el question-service y lo consume el review-service para saber que
 * preguntas puede asignar el administrador (HU04).
 */
public record QuestionSubmittedForReviewEvent(String questionId, String authorLogin, String directQuestion,
                                              Instant occurredAt) implements DomainEvent {

    @Override
    public String routingKey() {
        return EventTopics.QUESTION_SUBMITTED_FOR_REVIEW;
    }
}
