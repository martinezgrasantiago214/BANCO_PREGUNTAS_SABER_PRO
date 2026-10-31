package co.unicauca.saberpro.outbox;

import co.unicauca.saberpro.events.EventTopology;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Envia al exchange {@code saberpro.events} y espera el <i>publisher confirm</i>
 * de RabbitMQ: solo cuando el broker confirma que guardo el mensaje se marca
 * como publicado en la outbox. Asi se obtiene entrega "al menos una vez".
 */
public class RabbitOutboxMessageSender implements OutboxMessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final Duration confirmTimeout;

    public RabbitOutboxMessageSender(RabbitTemplate rabbitTemplate, Duration confirmTimeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.confirmTimeout = confirmTimeout;
    }

    @Override
    public void send(String routingKey, Object event) throws Exception {
        CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(EventTopology.EXCHANGE, routingKey, event, correlation);
        CorrelationData.Confirm confirm = correlation.getFuture()
                .get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ rechazó el mensaje: " + confirm.getReason());
        }
    }
}
