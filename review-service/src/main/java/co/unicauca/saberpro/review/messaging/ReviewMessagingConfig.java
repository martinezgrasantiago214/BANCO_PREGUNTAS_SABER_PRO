package co.unicauca.saberpro.review.messaging;

import co.unicauca.saberpro.common.events.EventTopics;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Colas propias del review-service y su enlace con el exchange de eventos. */
@Configuration
public class ReviewMessagingConfig {

    public static final String QUESTION_SUBMITTED_QUEUE = "review-service.question-submitted";

    @Bean
    public Queue questionSubmittedQueue() {
        return new Queue(QUESTION_SUBMITTED_QUEUE, true);
    }

    @Bean
    public Binding questionSubmittedBinding(Queue questionSubmittedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(questionSubmittedQueue).to(eventsExchange)
                .with(EventTopics.QUESTION_SUBMITTED_FOR_REVIEW);
    }
}
