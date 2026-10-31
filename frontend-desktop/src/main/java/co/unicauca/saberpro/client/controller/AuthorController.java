package co.unicauca.saberpro.client.controller;

import co.unicauca.saberpro.client.api.SaberProApiFacade;
import co.unicauca.saberpro.client.api.dto.*;
import co.unicauca.saberpro.client.model.AppChange;
import co.unicauca.saberpro.client.model.AppEvents;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/** Controlador del autor de preguntas (HU01, HU02, HU03). */
public class AuthorController {

    private final SaberProApiFacade api;
    private final AppEvents events;

    public AuthorController(SaberProApiFacade api, AppEvents events) {
        this.api = api;
        this.events = events;
    }

    public void save(Component view, String questionId, QuestionRequestDto request, Consumer<QuestionDto> onSuccess) {
        UiTask.run(view, () -> questionId == null ? api.createQuestion(request) : api.updateQuestion(questionId, request),
                q -> {
                    events.publish(AppChange.QUESTIONS);
                    onSuccess.accept(q);
                });
    }

    public void search(Component view, String state, String topic, String keyword, int page, int size,
                       Consumer<PageDto<QuestionDto>> onSuccess) {
        UiTask.run(view, () -> api.myQuestions(state, topic, keyword, page, size), onSuccess);
    }

    public void submitForReview(Component view, String id, Consumer<QuestionDto> onSuccess) {
        UiTask.run(view, () -> api.submitForReview(id), q -> {
            events.publish(AppChange.QUESTIONS);
            events.publish(AppChange.REVIEWS);
            onSuccess.accept(q);
        });
    }

    public void delete(Component view, String id, Runnable onSuccess) {
        UiTask.run(view, () -> { api.deleteQuestion(id); return null; }, v -> {
            events.publish(AppChange.QUESTIONS);
            onSuccess.run();
        });
    }

    public void detail(Component view, String id, Consumer<QuestionDetail> onSuccess) {
        UiTask.run(view, () -> new QuestionDetail(api.question(id), api.observationsOf(id)), onSuccess);
    }

    public void states(Component view, Consumer<List<CatalogItemDto>> onSuccess) {
        UiTask.run(view, api::questionStates, onSuccess);
    }

    public record QuestionDetail(QuestionDto question, List<ObservationDto> observations) {
    }
}
