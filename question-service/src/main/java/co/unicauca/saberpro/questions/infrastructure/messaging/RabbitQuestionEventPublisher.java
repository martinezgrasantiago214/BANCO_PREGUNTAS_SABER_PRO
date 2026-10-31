package co.unicauca.saberpro.questions.infrastructure.messaging;

import co.unicauca.saberpro.events.EventTopology;
import co.unicauca.saberpro.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.questions.application.port.QuestionEventPublisherPort;
import co.unicauca.saberpro.questions.domain.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/** Adaptador de salida: publica los eventos de dominio en RabbitMQ. */
@Component
public class RabbitQuestionEventPublisher implements QuestionEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitQuestionEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public RabbitQuestionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishSubmittedForReview(Question q) {
        QuestionSubmittedForReviewEvent event = new QuestionSubmittedForReviewEvent(
                UUID.randomUUID().toString(), Instant.now(), q.getId(), q.getAuthorLogin(), q.getStatement(),
                q.getCompetence(), q.getTopic(), q.getSubtopic(), q.getDifficulty().name());
        rabbitTemplate.convertAndSend(EventTopology.EXCHANGE, EventTopology.QUESTION_SUBMITTED, event);
        log.info("Evento publicado {} para la pregunta {}", EventTopology.QUESTION_SUBMITTED, q.getId());
    }
}
