package co.unicauca.saberpro.common.messaging;

import co.unicauca.saberpro.common.events.DomainEvent;
import co.unicauca.saberpro.common.events.EventTopics;
import co.unicauca.saberpro.common.events.DomainEventListener;
import org.springframework.amqp.core.AmqpTemplate;

/**
 * Observador que reenvia cada evento de dominio al exchange de RabbitMQ, con
 * la clave de enrutamiento del evento, para que lo consuman los demas
 * microservicios.
 */
public class RabbitEventForwarder implements DomainEventListener {

    private final AmqpTemplate amqpTemplate;

    public RabbitEventForwarder(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    @Override
    public void onEvent(DomainEvent event) {
        amqpTemplate.convertAndSend(EventTopics.EXCHANGE, event.routingKey(), event);
    }
}
