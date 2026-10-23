package co.unicauca.saberpro.common.events;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;

import java.time.Instant;

/**
 * HU05: un revisor evaluo una pregunta (Aprobada/Rechazada) y dejo
 * observaciones. Lo publica el review-service; el question-service lo usa
 * para cambiar el estado de la pregunta y el notification-service para
 * avisar al autor y al administrador.
 */
public record QuestionEvaluatedEvent(String questionId, String authorLogin, String reviewerLogin,
                                     EvaluationVerdict verdict, String observations,
                                     Instant occurredAt) implements DomainEvent {

    @Override
    public String routingKey() {
        return EventTopics.QUESTION_EVALUATED;
    }
}
