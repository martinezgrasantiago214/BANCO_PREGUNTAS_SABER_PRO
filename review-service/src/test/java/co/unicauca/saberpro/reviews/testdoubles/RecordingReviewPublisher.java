package co.unicauca.saberpro.reviews.testdoubles;

import co.unicauca.saberpro.reviews.application.port.ReviewEventPublisherPort;
import co.unicauca.saberpro.reviews.domain.model.Observation;
import co.unicauca.saberpro.reviews.domain.model.QuestionReview;
import co.unicauca.saberpro.reviews.domain.model.ReviewerAssignment;

import java.util.ArrayList;
import java.util.List;

/** Espia: registra en orden el nombre de cada evento publicado. */
public class RecordingReviewPublisher implements ReviewEventPublisherPort {

    public final List<String> events = new ArrayList<>();

    @Override
    public void reviewersAssigned(QuestionReview review) {
        events.add("ASSIGNED:" + String.join(",", review.reviewerLogins()));
    }

    @Override
    public void evaluationSubmitted(QuestionReview review, ReviewerAssignment assignment) {
        events.add("EVALUATED:" + assignment.getReviewerLogin() + ":" + assignment.getVerdict());
    }

    @Override
    public void observationAdded(QuestionReview review, Observation observation) {
        events.add("OBSERVATION:" + observation.reviewerLogin());
    }

    @Override
    public void reviewCompleted(QuestionReview review) {
        events.add("COMPLETED:" + review.getFinalDecision());
    }
}
