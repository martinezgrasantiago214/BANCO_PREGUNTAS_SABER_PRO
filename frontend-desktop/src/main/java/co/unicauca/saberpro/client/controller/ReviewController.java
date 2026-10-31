package co.unicauca.saberpro.client.controller;

import co.unicauca.saberpro.client.api.SaberProApiFacade;
import co.unicauca.saberpro.client.api.dto.*;
import co.unicauca.saberpro.client.model.AppChange;
import co.unicauca.saberpro.client.model.AppEvents;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/** Controlador del administrador (HU04) y del revisor (HU05). */
public class ReviewController {

    public static final int MIN_REVIEWERS = 1;
    public static final int MAX_REVIEWERS = 3;

    private final SaberProApiFacade api;
    private final AppEvents events;

    public ReviewController(SaberProApiFacade api, AppEvents events) {
        this.api = api;
        this.events = events;
    }

    // ---------------------------------------------------------------- HU04
    public void pending(Component view, int page, int size, Consumer<PageDto<ReviewDto>> onSuccess) {
        UiTask.run(view, () -> api.pendingAssignment(page, size), onSuccess);
    }

    public void reviewers(Component view, Consumer<List<UserDto>> onSuccess) {
        UiTask.run(view, api::reviewers, onSuccess);
    }

    /** Valida en el cliente (1 a 3) para dar retroalimentacion inmediata; el servidor vuelve a validar. */
    public String validateSelection(List<String> logins) {
        if (logins.size() < MIN_REVIEWERS || logins.size() > MAX_REVIEWERS) {
            return "Seleccione entre " + MIN_REVIEWERS + " y " + MAX_REVIEWERS + " revisores.";
        }
        return null;
    }

    public void assign(Component view, String questionId, List<String> logins, Consumer<ReviewDto> onSuccess) {
        UiTask.run(view, () -> api.assignReviewers(questionId, logins), r -> {
            events.publish(AppChange.REVIEWS);
            events.publish(AppChange.QUESTIONS);
            onSuccess.accept(r);
        });
    }

    // ---------------------------------------------------------------- HU05
    public void assignedToMe(Component view, String status, int page, int size, Consumer<PageDto<ReviewDto>> onSuccess) {
        UiTask.run(view, () -> api.assignedToMe(status, page, size), onSuccess);
    }

    public void workspace(Component view, String questionId, Consumer<SaberProApiFacade.ReviewWorkspace> onSuccess) {
        UiTask.run(view, () -> api.reviewWorkspace(questionId), onSuccess);
    }

    public void evaluate(Component view, String questionId, EvaluationRequestDto evaluation,
                         Consumer<ReviewDto> onSuccess) {
        UiTask.run(view, () -> api.evaluate(questionId, evaluation), r -> {
            events.publish(AppChange.REVIEWS);
            onSuccess.accept(r);
        });
    }

    public void observe(Component view, String questionId, String text, Consumer<ObservationDto> onSuccess) {
        UiTask.run(view, () -> api.addObservation(questionId, text), o -> {
            events.publish(AppChange.REVIEWS);
            onSuccess.accept(o);
        });
    }
}
