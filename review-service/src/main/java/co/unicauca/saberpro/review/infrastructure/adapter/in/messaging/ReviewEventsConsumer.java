package co.unicauca.saberpro.review.infrastructure.adapter.in.messaging;

import co.unicauca.saberpro.common.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.review.application.port.in.RegisterSubmissionUseCase;
import co.unicauca.saberpro.review.domain.exception.ConcurrentReviewUpdateException;
import co.unicauca.saberpro.review.domain.exception.ReviewDomainException;
import co.unicauca.saberpro.review.domain.model.QuestionSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada (mensajeria): traduce el evento de integracion
 * question.submitted al objeto de valor del dominio y llama al caso de uso.
 */
@Component
public class ReviewEventsConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewEventsConsumer.class);

    private final RegisterSubmissionUseCase registerSubmission;

    public ReviewEventsConsumer(RegisterSubmissionUseCase registerSubmission) {
        this.registerSubmission = registerSubmission;
    }

    /** HU02 -> HU04 y HU06: una version de la pregunta quedo Pendiente de revision. */
    @RabbitListener(queues = ReviewMessagingConfig.QUESTION_SUBMITTED_QUEUE)
    public void onQuestionSubmitted(QuestionSubmittedForReviewEvent event) {
        try {
            QuestionSubmission submission = new QuestionSubmission(event.questionId(), event.questionVersion(),
                    event.authorLogin(), event.directQuestion(), event.occurredAt());
            boolean changed = registerSubmission.register(submission);
            LOGGER.info("Pregunta {} version {} {}", submission.questionId(), submission.questionVersion(),
                    changed ? "registrada para revision" : "ya estaba registrada (evento repetido)");
        } catch (ConcurrentReviewUpdateException ex) {
            // Conflicto pasajero: se relanza para que RabbitMQ vuelva a entregar el mensaje.
            throw ex;
        } catch (ReviewDomainException ex) {
            // Un evento que viola las reglas se confirma igual: reintentarlo daria el mismo resultado.
            LOGGER.warn("{} Evento ignorado: {}", ex.getMessage(), event);
        }
    }
}
