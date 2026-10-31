package co.unicauca.saberpro.questions.application.port;

import co.unicauca.saberpro.questions.domain.model.Question;

/** Puerto de salida de mensajeria: la aplicacion no conoce RabbitMQ. */
public interface QuestionEventPublisherPort {

    void publishSubmittedForReview(Question question);
}
