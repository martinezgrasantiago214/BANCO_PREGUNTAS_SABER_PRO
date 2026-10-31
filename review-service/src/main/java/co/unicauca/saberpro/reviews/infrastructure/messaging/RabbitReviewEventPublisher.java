package co.unicauca.saberpro.reviews.infrastructure.messaging;

import co.unicauca.saberpro.events.*;
import co.unicauca.saberpro.reviews.application.port.ReviewEventPublisherPort;
import co.unicauca.saberpro.reviews.domain.model.Observation;
import co.unicauca.saberpro.reviews.domain.model.QuestionReview;
import co.unicauca.saberpro.reviews.domain.model.ReviewerAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/** Adaptador de salida: traduce los hechos del dominio a eventos de integracion. */
@Component
public class RabbitReviewEventPublisher implements ReviewEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitReviewEventPublisher.class);
    private final RabbitTemplate rabbit;

    public RabbitReviewEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void reviewersAssigned(QuestionReview r) {
        send(EventTopology.REVIEWERS_ASSIGNED, new ReviewersAssignedEvent(id(), Instant.now(), r.getQuestionId(),
                r.getAuthorLogin(), r.getStatement(), r.reviewerLogins(), r.getAssignedBy()));
    }

    @Override
    public void evaluationSubmitted(QuestionReview r, ReviewerAssignment a) {
        send(EventTopology.EVALUATION_SUBMITTED, new ReviewEvaluationSubmittedEvent(id(), Instant.now(),
                r.getQuestionId(), r.getAuthorLogin(), r.getStatement(), a.getReviewerLogin(),
                a.getVerdict().name(), a.averageScore()));
    }

    @Override
    public void observationAdded(QuestionReview r, Observation o) {
        send(EventTopology.OBSERVATION_ADDED, new ObservationAddedEvent(id(), Instant.now(), r.getQuestionId(),
                r.getAuthorLogin(), r.getStatement(), o.reviewerLogin(), o.text()));
    }

    @Override
    public void reviewCompleted(QuestionReview r) {
        send(EventTopology.REVIEW_COMPLETED, new ReviewCompletedEvent(id(), Instant.now(), r.getQuestionId(),
                r.getAuthorLogin(), r.getStatement(), r.getFinalDecision().name(), r.reviewerLogins()));
    }

    private void send(String routingKey, IntegrationEvent event) {
        rabbit.convertAndSend(EventTopology.EXCHANGE, routingKey, event);
        log.info("Evento publicado {} para la pregunta {}", routingKey, event.questionId());
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }
}
