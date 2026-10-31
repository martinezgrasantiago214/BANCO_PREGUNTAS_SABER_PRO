package co.unicauca.saberpro.questions.infrastructure.messaging;

import co.unicauca.saberpro.events.EventTopology;
import co.unicauca.saberpro.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.outbox.OutboxEventWriter;
import co.unicauca.saberpro.questions.application.port.QuestionEventPublisherPort;
import co.unicauca.saberpro.questions.domain.model.Question;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Adaptador de salida: traduce los hechos del dominio a eventos de integracion
 * y los deja en la outbox, dentro de la transaccion del caso de uso. El
 * {@code OutboxRelay} los entrega despues a RabbitMQ. Si el broker esta caido
 * en ese momento, el evento no se pierde: espera en la base de datos.
 */
@Component
public class OutboxQuestionEventPublisher implements QuestionEventPublisherPort {

    private final OutboxEventWriter outbox;

    public OutboxQuestionEventPublisher(OutboxEventWriter outbox) {
        this.outbox = outbox;
    }

    @Override
    public void publishSubmittedForReview(Question q) {
        outbox.enqueue(EventTopology.QUESTION_SUBMITTED, new QuestionSubmittedForReviewEvent(
                UUID.randomUUID().toString(), Instant.now(), q.getId(), q.getAuthorLogin(), q.getStatement(),
                q.getCompetence(), q.getTopic(), q.getSubtopic(), q.getDifficulty().name()));
    }
}
