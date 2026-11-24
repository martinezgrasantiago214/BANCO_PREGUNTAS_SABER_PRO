package co.unicauca.saberpro.common.events;

import java.time.Instant;

/**
 * HU06: el autor saco una version mejorada de una pregunta Rechazada; la
 * pregunta volvio a Borrador con el numero de version siguiente. Lo publica
 * el question-service (queda en el log de eventos y en el broker para quien
 * le interese, p. ej. auditoria).
 */
public record QuestionRevisedEvent(String questionId, int questionVersion, String authorLogin,
                                   String directQuestion, Instant occurredAt) implements DomainEvent {

    @Override
    public String routingKey() {
        return EventTopics.QUESTION_REVISED;
    }
}
