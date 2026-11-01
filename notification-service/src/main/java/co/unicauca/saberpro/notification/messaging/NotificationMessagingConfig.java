package co.unicauca.saberpro.notification.messaging;

import co.unicauca.saberpro.common.events.EventTopics;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Colas del notification-service. Si se levantan varias instancias del
 * servicio, todas leen de las mismas colas y RabbitMQ reparte los mensajes
 * entre ellas (consumidores competidores): asi escala el envio de correos.
 */
@Configuration
public class NotificationMessagingConfig {

    public static final String REVIEWERS_ASSIGNED_QUEUE = "notification-service.reviewers-assigned";
    public static final String QUESTION_EVALUATED_QUEUE = "notification-service.question-evaluated";

    @Bean
    public Queue reviewersAssignedQueue() {
        return new Queue(REVIEWERS_ASSIGNED_QUEUE, true);
    }

    @Bean
    public Queue notificationQuestionEvaluatedQueue() {
        return new Queue(QUESTION_EVALUATED_QUEUE, true);
    }

    @Bean
    public Binding reviewersAssignedBinding(Queue reviewersAssignedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(reviewersAssignedQueue).to(eventsExchange).with(EventTopics.REVIEWERS_ASSIGNED);
    }

    @Bean
    public Binding notificationQuestionEvaluatedBinding(Queue notificationQuestionEvaluatedQueue,
                                                        TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationQuestionEvaluatedQueue).to(eventsExchange)
                .with(EventTopics.QUESTION_EVALUATED);
    }
}
