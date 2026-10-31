package co.unicauca.saberpro.questions.infrastructure.messaging;

import co.unicauca.saberpro.events.EventTopology;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ de question-service: publica en el exchange topic y
 * consume de SU cola los eventos del contexto de revision. Los mensajes que
 * fallan tras los reintentos van a una cola de mensajes muertos (DLQ).
 */
@Configuration
public class RabbitConfig {

    public static final String QUEUE = "question-service.review-events";
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
    Queue reviewEventsQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(EventTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue reviewEventsDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Declarables reviewEventsBindings(Queue reviewEventsQueue, Queue reviewEventsDlq,
                                     TopicExchange eventsExchange, DirectExchange deadLetterExchange) {
        return new Declarables(
                BindingBuilder.bind(reviewEventsQueue).to(eventsExchange).with(EventTopology.REVIEWERS_ASSIGNED),
                BindingBuilder.bind(reviewEventsQueue).to(eventsExchange).with(EventTopology.REVIEW_COMPLETED),
                BindingBuilder.bind(reviewEventsDlq).to(deadLetterExchange).with(DLQ));
    }

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper, "co.unicauca.saberpro.events");
    }
}
