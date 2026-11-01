package co.unicauca.saberpro.question.messaging;

import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.web.PendingIssueException;
import co.unicauca.saberpro.question.application.QuestionApplicationService;
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

    /** HU05: un revisor evaluo la pregunta; hay que pasarla a Aprobada o Rechazada. */
    @RabbitListener(queues = QuestionMessagingConfig.QUESTION_EVALUATED_QUEUE)
    public void onQuestionEvaluated(QuestionEvaluatedEvent event) {
        try {
            service.applyEvaluation(event);
        } catch (PendingIssueException ex) {
            // Se confirma el mensaje para no reintentarlo sin fin mientras el caso de uso no exista.
            LOGGER.warn("{} Evento ignorado: {}", ex.getMessage(), event);
        }
    }
}
