package co.unicauca.saberpro.review.application.port.out;

import co.unicauca.saberpro.review.domain.event.ReviewDomainEvent;

import java.util.List;

/** Puerto de salida: publica los eventos de dominio para los demas microservicios. */
public interface ReviewEventPublisher {

    void publish(List<ReviewDomainEvent> events);
}
