package co.unicauca.saberpro.notifications.infrastructure.messaging;

import co.unicauca.saberpro.events.EventTopology;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Una sola cola compartida por TODAS las replicas de notification-service:
 * RabbitMQ reparte los mensajes entre ellas (competing consumers), que es el
 * mecanismo de escalabilidad horizontal del escenario de calidad.
 */
@Configuration
public class RabbitConfig {

    public static final String QUEUE = "notification-service.events";
    public static final String DLQ = QUEUE + ".dlq";

    @Bean
    TopicExchange eventsExchange() {
        return ExchangeBuilder.topicExchange(EventTopology.EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(EventTopology.DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(EventTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue notificationDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Declarables notificationBindings(Queue notificationQueue, Queue notificationDlq,
                                     TopicExchange eventsExchange, DirectExchange deadLetterExchange) {
        return new Declarables(
                BindingBuilder.bind(notificationQueue).to(eventsExchange).with(EventTopology.QUESTION_SUBMITTED),
                BindingBuilder.bind(notificationQueue).to(eventsExchange).with("review.#"),
                BindingBuilder.bind(notificationDlq).to(deadLetterExchange).with(DLQ));
    }

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper, "co.unicauca.saberpro.events");
    }
}
