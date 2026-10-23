package co.unicauca.saberpro.common.events;

import java.time.Instant;

/** HU01: una pregunta valida quedo almacenada en estado Borrador. Lo publica el question-service. */
public record QuestionCreatedEvent(String questionId, String authorLogin, String directQuestion,
                                   String topic, Instant occurredAt) implements DomainEvent {

    @Override
    public String routingKey() {
        return EventTopics.QUESTION_CREATED;
    }
}
