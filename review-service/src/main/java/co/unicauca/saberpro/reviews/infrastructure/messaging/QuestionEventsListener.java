package co.unicauca.saberpro.reviews.infrastructure.messaging;

import co.unicauca.saberpro.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.reviews.application.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada: crea el proceso de revision cuando un autor envia su pregunta. */
@Component
@RabbitListener(queues = RabbitConfig.QUEUE)
public class QuestionEventsListener {

    private static final Logger log = LoggerFactory.getLogger(QuestionEventsListener.class);
    private final ReviewService reviewService;

    public QuestionEventsListener(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @RabbitHandler
    public void on(QuestionSubmittedForReviewEvent e) {
        log.info("Pregunta {} recibida para revisión (autor {})", e.questionId(), e.authorLogin());
        reviewService.registerSubmission(e.questionId(), e.authorLogin(), e.directQuestion(), e.competence(), e.topic());
    }

    @RabbitHandler(isDefault = true)
    public void onUnknown(Object payload) {
        log.warn("Evento ignorado por review-service: {}", payload);
    }
}
