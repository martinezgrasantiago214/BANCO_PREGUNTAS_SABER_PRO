package co.unicauca.saberpro.questions.infrastructure.messaging;

import co.unicauca.saberpro.events.ReviewCompletedEvent;
import co.unicauca.saberpro.events.ReviewersAssignedEvent;
import co.unicauca.saberpro.questions.application.QuestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada: el Ciclo de Vida reacciona a lo que decide el
 * contexto de Revision por Pares, sin acoplamiento sincrono entre servicios.
 */
@Component
@RabbitListener(queues = RabbitConfig.QUEUE)
public class ReviewEventsListener {

    private static final Logger log = LoggerFactory.getLogger(ReviewEventsListener.class);
    private final QuestionService questionService;

    public ReviewEventsListener(QuestionService questionService) {
        this.questionService = questionService;
    }

    @RabbitHandler
    public void on(ReviewersAssignedEvent event) {
        log.info("Revisores asignados a {}: {}", event.questionId(), event.reviewerLogins());
        questionService.markInReview(event.questionId());
    }

    @RabbitHandler
    public void on(ReviewCompletedEvent event) {
        log.info("Revisión de {} finalizada: {}", event.questionId(), event.decision());
        questionService.applyReviewDecision(event.questionId(), "APROBADA".equals(event.decision()));
    }

    @RabbitHandler(isDefault = true)
    public void onUnknown(Object payload) {
        log.warn("Evento ignorado por question-service: {}", payload);
    }
}
