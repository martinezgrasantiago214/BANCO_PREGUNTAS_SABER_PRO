package co.unicauca.saberpro.question.messaging;

import co.unicauca.saberpro.common.events.EventTopics;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Colas propias del question-service y su enlace con el exchange de eventos. */
@Configuration
public class QuestionMessagingConfig {

    public static final String QUESTION_EVALUATED_QUEUE = "question-service.question-evaluated";

    @Bean
    public Queue questionEvaluatedQueue() {
        return new Queue(QUESTION_EVALUATED_QUEUE, true);
    }

    @Bean
    public Binding questionEvaluatedBinding(Queue questionEvaluatedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(questionEvaluatedQueue).to(eventsExchange).with(EventTopics.QUESTION_EVALUATED);
    }
}
