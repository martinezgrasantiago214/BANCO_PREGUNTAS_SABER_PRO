package co.unicauca.saberpro.review.infrastructure.adapter.out.events;

import co.unicauca.saberpro.common.events.DomainEvent;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.events.QuestionResubmittedForReviewEvent;
import co.unicauca.saberpro.common.events.ReviewersAssignedEvent;
import co.unicauca.saberpro.review.application.port.out.ReviewEventPublisher;
import co.unicauca.saberpro.review.domain.event.QuestionEvaluated;
import co.unicauca.saberpro.review.domain.event.ReviewDomainEvent;
import co.unicauca.saberpro.review.domain.event.ReviewRoundReopened;
import co.unicauca.saberpro.review.domain.event.ReviewersAssigned;

import java.util.List;

/**
 * Adaptador de salida del puerto {@link ReviewEventPublisher}: traduce los
 * eventos del dominio (internos del contexto) a los eventos de integracion
 * del modulo common (el contrato publico entre microservicios) y los
 * entrega al DomainEventBus (patron Observer), cuyo observador
 * RabbitEventForwarder los envia al exchange saberpro.events.
 *
 * Asi el modelo de dominio puede cambiar sin romper a los consumidores
 * (question-service y notification-service): el contrato es este mapeo.
 */
public class DomainEventBusReviewPublisher implements ReviewEventPublisher {

    private final DomainEventBus bus;

    public DomainEventBusReviewPublisher(DomainEventBus bus) {
        this.bus = bus;
    }

    @Override
    public void publish(List<ReviewDomainEvent> events) {
        for (ReviewDomainEvent event : events) {
            bus.publish(toIntegrationEvent(event));
        }
    }

    static DomainEvent toIntegrationEvent(ReviewDomainEvent event) {
        if (event instanceof ReviewersAssigned e) {
            return new ReviewersAssignedEvent(e.questionId(), e.directQuestion(), e.reviewerLogins(), e.assignedBy(),
                    e.occurredAt());
        }
        if (event instanceof QuestionEvaluated e) {
            return new QuestionEvaluatedEvent(e.questionId(), e.questionVersion(), e.authorLogin(),
                    e.directQuestion(), e.reviewerLogin(), e.verdict(), e.observations(), e.occurredAt());
        }
        if (event instanceof ReviewRoundReopened e) {
            return new QuestionResubmittedForReviewEvent(e.questionId(), e.questionVersion(), e.round(),
                    e.authorLogin(), e.directQuestion(), e.previousReviewers(), e.previousObservations(),
                    e.occurredAt());
        }
        throw new IllegalArgumentException("Evento de dominio sin traduccion: " + event);
    }
}
