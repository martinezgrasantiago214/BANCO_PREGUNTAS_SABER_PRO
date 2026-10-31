package co.unicauca.saberpro.reviews.infrastructure.messaging;

import co.unicauca.saberpro.events.EventTopology;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** review-service consume "question.submitted" y publica los eventos de revision. */
@Configuration
public class RabbitConfig {

    public static final String QUEUE = "review-service.question-events";
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
    Queue questionEventsQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(EventTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue questionEventsDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Declarables questionEventsBindings(Queue questionEventsQueue, Queue questionEventsDlq,
                                       TopicExchange eventsExchange, DirectExchange deadLetterExchange) {
        return new Declarables(
                BindingBuilder.bind(questionEventsQueue).to(eventsExchange).with(EventTopology.QUESTION_SUBMITTED),
                BindingBuilder.bind(questionEventsDlq).to(deadLetterExchange).with(DLQ));
    }

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper, "co.unicauca.saberpro.events");
    }
}
