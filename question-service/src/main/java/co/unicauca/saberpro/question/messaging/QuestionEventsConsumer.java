package co.unicauca.saberpro.question.messaging;

import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.common.web.ResourceNotFoundException;
import co.unicauca.saberpro.question.application.QuestionApplicationService;
import co.unicauca.saberpro.question.domain.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Eventos de otros servicios que le interesan al question-service. */
@Component
public class QuestionEventsConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionEventsConsumer.class);

    private final QuestionApplicationService service;

    public QuestionEventsConsumer(QuestionApplicationService service) {
        this.service = service;
    }

    /**
     * HU05 (PSP-48): un revisor evaluo la pregunta; hay que pasarla a Aprobada
     * o Rechazada.
     *
     * Si el evento no se puede aplicar (la pregunta no existe o su estado no
     * lo permite), se registra y se confirma el mensaje: reintentarlo daria
     * siempre el mismo resultado. Los errores tecnicos (p. ej. la base de
     * datos) se propagan para que RabbitMQ lo vuelva a entregar.
     */
    @RabbitListener(queues = QuestionMessagingConfig.QUESTION_EVALUATED_QUEUE)
    public void onQuestionEvaluated(QuestionEvaluatedEvent event) {
        try {
            Question question = service.applyEvaluation(event);
            LOGGER.info("Pregunta {} en estado {} por la evaluacion de {}", question.getId(),
                    question.getStatus().getLabel(), event.reviewerLogin());
        } catch (BusinessException | ResourceNotFoundException ex) {
            LOGGER.warn("Evaluacion no aplicada ({}): {}", ex.getMessage(), event);
        }
    }
}
