package co.unicauca.saberpro.notification.messaging;

import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.events.ReviewersAssignedEvent;
import co.unicauca.saberpro.common.web.PendingIssueException;
import co.unicauca.saberpro.notification.application.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Escucha los eventos que deben generar una notificacion. */
@Component
public class NotificationEventsConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventsConsumer.class);

    private final NotificationService service;

    public NotificationEventsConsumer(NotificationService service) {
        this.service = service;
    }

    @RabbitListener(queues = NotificationMessagingConfig.REVIEWERS_ASSIGNED_QUEUE)
    public void onReviewersAssigned(ReviewersAssignedEvent event) {
        try {
            service.onReviewersAssigned(event);
        } catch (PendingIssueException ex) {
            LOGGER.warn("{} Evento ignorado: {}", ex.getMessage(), event);
        }
    }

    @RabbitListener(queues = NotificationMessagingConfig.QUESTION_EVALUATED_QUEUE)
    public void onQuestionEvaluated(QuestionEvaluatedEvent event) {
        try {
            service.onQuestionEvaluated(event);
        } catch (PendingIssueException ex) {
            LOGGER.warn("{} Evento ignorado: {}", ex.getMessage(), event);
        }
    }
}
