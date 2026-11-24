package co.unicauca.saberpro.review.application.service;

import co.unicauca.saberpro.review.application.port.in.ReviewQueries;
import co.unicauca.saberpro.review.application.port.out.QuestionReviewRepository;
import co.unicauca.saberpro.review.domain.exception.ReviewNotFoundException;
import co.unicauca.saberpro.review.domain.exception.ReviewerNotAllowedException;
import co.unicauca.saberpro.review.domain.model.Evaluation;
import co.unicauca.saberpro.review.domain.model.QuestionReview;
import co.unicauca.saberpro.review.domain.model.ReviewRound;
import co.unicauca.saberpro.review.domain.model.ReviewerAssignment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Consultas del review-service (HU04, HU05 y HU06). No modifican nada. */
public class ReviewQueryService implements ReviewQueries {

    private final QuestionReviewRepository repository;

    public ReviewQueryService(QuestionReviewRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<QuestionReview> pendingQuestions() {
        return repository.findAwaitingDecision().stream()
                .sorted(Comparator.comparing((QuestionReview r) -> r.currentRound().submittedAt()))
                .toList();
    }

    @Override
    public List<AssignedReview> assignedTo(String reviewerLogin) {
        String reviewer = Texts.require(reviewerLogin, "Debe indicar el login del revisor.");
        List<AssignedReview> result = new ArrayList<>();
        for (QuestionReview review : repository.findByReviewer(reviewer)) {
            ReviewRound current = review.currentRound();
            for (ReviewRound round : review.rounds()) {
                for (ReviewerAssignment assignment : round.assignments()) {
                    if (assignment.isFor(reviewer)) {
                        result.add(new AssignedReview(review.questionId(), round.number(), round.questionVersion(),
                                round.directQuestion(), assignment, round.status(), round == current));
                    }
                }
            }
        }
        result.sort(Comparator.comparing((AssignedReview a) -> a.assignment().assignedAt()).reversed());
        return result;
    }

    @Override
    public QuestionReview history(String questionId, String requesterLogin, boolean requesterIsAdmin) {
        String id = Texts.require(questionId, "Debe indicar la pregunta.");
        QuestionReview review = repository.findById(id).orElseThrow(() -> new ReviewNotFoundException(id));
        if (!review.canBeConsultedBy(requesterLogin, requesterIsAdmin)) {
            throw new ReviewerNotAllowedException(
                    "Solo el autor, los administradores y los revisores de la pregunta pueden ver su historial.");
        }
        return review;
    }

    @Override
    public List<Evaluation> evaluations(String questionId, String requesterLogin, boolean requesterIsAdmin) {
        return history(questionId, requesterLogin, requesterIsAdmin).evaluations();
    }
}
