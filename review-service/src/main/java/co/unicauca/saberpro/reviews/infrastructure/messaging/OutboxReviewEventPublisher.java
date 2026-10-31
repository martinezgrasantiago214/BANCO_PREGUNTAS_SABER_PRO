package co.unicauca.saberpro.reviews.infrastructure.messaging;

import co.unicauca.saberpro.events.*;
import co.unicauca.saberpro.reviews.application.port.ReviewEventPublisherPort;
import co.unicauca.saberpro.reviews.domain.model.Observation;
import co.unicauca.saberpro.reviews.domain.model.QuestionReview;
import co.unicauca.saberpro.reviews.domain.model.ReviewerAssignment;
import co.unicauca.saberpro.outbox.OutboxEventWriter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Adaptador de salida: traduce los hechos del dominio a eventos de integracion
 * y los deja en la outbox, dentro de la transaccion del caso de uso. Asi, por
 * ejemplo, la evaluacion de un revisor, su observacion y la decision final se
 * guardan junto con sus eventos o no se guarda nada.
 */
@Component
public class OutboxReviewEventPublisher implements ReviewEventPublisherPort {

    private final OutboxEventWriter outbox;

    public OutboxReviewEventPublisher(OutboxEventWriter outbox) {
        this.outbox = outbox;
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
        outbox.enqueue(routingKey, event);
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }
}
