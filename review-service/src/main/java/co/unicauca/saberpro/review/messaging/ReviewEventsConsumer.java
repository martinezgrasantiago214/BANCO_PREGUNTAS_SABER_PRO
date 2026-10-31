package co.unicauca.saberpro.review.messaging;

import co.unicauca.saberpro.common.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.common.web.PendingIssueException;
import co.unicauca.saberpro.review.application.ReviewApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Eventos de otros servicios que le interesan al review-service. */
@Component
public class ReviewEventsConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewEventsConsumer.class);

    private final ReviewApplicationService service;

    public ReviewEventsConsumer(ReviewApplicationService service) {
        this.service = service;
    }

    /** HU02 -> HU04: una pregunta quedo Pendiente de revision y el administrador debe poder asignarla. */
    @RabbitListener(queues = ReviewMessagingConfig.QUESTION_SUBMITTED_QUEUE)
    public void onQuestionSubmitted(QuestionSubmittedForReviewEvent event) {
        try {
            service.registerPendingQuestion(event);
        } catch (PendingIssueException ex) {
            // Se confirma el mensaje para no reintentarlo sin fin mientras el caso de uso no exista.
            LOGGER.warn("{} Evento ignorado: {}", ex.getMessage(), event);
        }
    }
}
