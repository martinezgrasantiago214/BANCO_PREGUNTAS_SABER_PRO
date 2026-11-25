package co.unicauca.saberpro.common.events;

import java.time.Instant;

/**
 * HU02: el autor paso una pregunta de Borrador a Pendiente de revision. Lo
 * publica el question-service y lo consume el review-service para saber que
 * preguntas puede asignar el administrador (HU04).
 *
 * HU06 (tercer corte): trae la version de la pregunta. La version 1 abre la
 * primera ronda de revision; una version mayor es una version mejorada
 * despues de un rechazo y abre una ronda nueva en el review-service.
 * Si el mensaje no trae version (eventos del segundo corte), se asume 1.
 */
public record QuestionSubmittedForReviewEvent(String questionId, int questionVersion, String authorLogin,
                                              String directQuestion, Instant occurredAt) implements DomainEvent {

    public QuestionSubmittedForReviewEvent {
        questionVersion = Math.max(questionVersion, 1);
    }

    /**
     * Firma del segundo corte (sin version): equivale a la version 1. Se
     * conserva para que los productores que aun no conocen la HU06 compilen.
     */
    public QuestionSubmittedForReviewEvent(String questionId, String authorLogin, String directQuestion,
                                           Instant occurredAt) {
        this(questionId, 1, authorLogin, directQuestion, occurredAt);
    }

    @Override
    public String routingKey() {
        return EventTopics.QUESTION_SUBMITTED_FOR_REVIEW;
    }
}
