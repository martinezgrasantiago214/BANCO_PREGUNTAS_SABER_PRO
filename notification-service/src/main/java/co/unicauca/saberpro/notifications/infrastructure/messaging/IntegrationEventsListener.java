package co.unicauca.saberpro.notifications.infrastructure.messaging;

import co.unicauca.saberpro.events.*;
import co.unicauca.saberpro.notifications.application.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitConfig.QUEUE)
public class IntegrationEventsListener {

    private static final Logger log = LoggerFactory.getLogger(IntegrationEventsListener.class);
    private final NotificationService service;

    public IntegrationEventsListener(NotificationService service) {
        this.service = service;
    }

    @RabbitHandler
    public void on(QuestionSubmittedForReviewEvent e) { process(e); }

    @RabbitHandler
    public void on(ReviewersAssignedEvent e) { process(e); }

    @RabbitHandler
    public void on(ReviewEvaluationSubmittedEvent e) { process(e); }

    @RabbitHandler
    public void on(ObservationAddedEvent e) { process(e); }

    @RabbitHandler
    public void on(ReviewCompletedEvent e) { process(e); }

    @RabbitHandler(isDefault = true)
    public void onUnknown(Object payload) {
        log.warn("Evento ignorado por notification-service: {}", payload);
    }

    private void process(IntegrationEvent e) {
        int count = service.handle(e).size();
        log.info("{} de la pregunta {} -> {} notificación(es)", e.getClass().getSimpleName(), e.questionId(), count);
    }
}
