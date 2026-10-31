package co.unicauca.saberpro.reviews.application.port;

import co.unicauca.saberpro.reviews.domain.model.Observation;
import co.unicauca.saberpro.reviews.domain.model.QuestionReview;
import co.unicauca.saberpro.reviews.domain.model.ReviewerAssignment;

/** Puerto de salida: eventos que este contexto publica para los demas. */
public interface ReviewEventPublisherPort {

    void reviewersAssigned(QuestionReview review);

    void evaluationSubmitted(QuestionReview review, ReviewerAssignment assignment);

    void observationAdded(QuestionReview review, Observation observation);

    void reviewCompleted(QuestionReview review);
}
