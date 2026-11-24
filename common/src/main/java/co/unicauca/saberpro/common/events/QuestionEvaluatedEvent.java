package co.unicauca.saberpro.common.events;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;

import java.time.Instant;

/**
 * HU05: un revisor evaluo una pregunta (Aprobada/Rechazada) y dejo
 * observaciones. Lo publica el review-service; el question-service lo usa
 * para cambiar el estado de la pregunta y el notification-service para
 * avisar al autor y al administrador.
 *
 * Trae el texto de la pregunta (directQuestion) para que la notificacion
 * diga cual se evaluo sin consultar al question-service, y la version
 * evaluada (HU06) para que el question-service ignore un veredicto atrasado
 * de una version anterior.
 */
public record QuestionEvaluatedEvent(String questionId, int questionVersion, String authorLogin,
                                     String directQuestion, String reviewerLogin, EvaluationVerdict verdict,
                                     String observations, Instant occurredAt) implements DomainEvent {

    public QuestionEvaluatedEvent {
        questionVersion = Math.max(questionVersion, 1);
    }

    /**
     * Firma del segundo corte (sin version): equivale a la version 1. Se
     * conserva para que los productores que aun no conocen la HU06 compilen.
     */
    public QuestionEvaluatedEvent(String questionId, String authorLogin, String directQuestion,
                                  String reviewerLogin, EvaluationVerdict verdict, String observations,
                                  Instant occurredAt) {
        this(questionId, 1, authorLogin, directQuestion, reviewerLogin, verdict, observations, occurredAt);
    }

    @Override
    public String routingKey() {
        return EventTopics.QUESTION_EVALUATED;
    }
}
