package co.unicauca.saberpro.questions.testdoubles;

import co.unicauca.saberpro.questions.application.port.QuestionEventPublisherPort;
import co.unicauca.saberpro.questions.domain.model.Question;

import java.util.ArrayList;
import java.util.List;

/** Espia que registra los eventos publicados. */
public class RecordingEventPublisher implements QuestionEventPublisherPort {

    public final List<String> submitted = new ArrayList<>();

    @Override
    public void publishSubmittedForReview(Question question) {
        submitted.add(question.getId());
    }
}
