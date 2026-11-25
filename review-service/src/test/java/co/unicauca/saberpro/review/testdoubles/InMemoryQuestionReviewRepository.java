package co.unicauca.saberpro.review.testdoubles;

import co.unicauca.saberpro.review.application.port.out.QuestionReviewRepository;
import co.unicauca.saberpro.review.domain.exception.ConcurrentReviewUpdateException;
import co.unicauca.saberpro.review.domain.model.QuestionReview;
import co.unicauca.saberpro.review.domain.model.ReviewRound;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Doble de prueba del puerto de persistencia. Guarda copias del agregado (no
 * la misma instancia), como haria una base de datos, y aplica el mismo
 * bloqueo optimista que el adaptador SQLite.
 */
public class InMemoryQuestionReviewRepository implements QuestionReviewRepository {

    private final Map<String, QuestionReview> store = new ConcurrentHashMap<>();
    private int saves;

    @Override
    public Optional<QuestionReview> findById(String questionId) {
        return Optional.ofNullable(store.get(questionId)).map(r -> copy(r, r.version()));
    }

    @Override
    public synchronized void save(QuestionReview review) {
        QuestionReview stored = store.get(review.questionId());
        long expected = stored == null ? 0 : stored.version();
        if (review.version() != expected) {
            throw new ConcurrentReviewUpdateException(review.questionId());
        }
        store.put(review.questionId(), copy(review, expected + 1));
        saves++;
    }

    @Override
    public List<QuestionReview> findAwaitingDecision() {
        return store.values().stream().filter(QuestionReview::isAwaitingDecision).map(r -> copy(r, r.version())).toList();
    }

    @Override
    public List<QuestionReview> findByReviewer(String reviewerLogin) {
        return store.values().stream()
                .filter(r -> r.rounds().stream().anyMatch(round -> round.isAssigned(reviewerLogin)))
                .map(r -> copy(r, r.version())).toList();
    }

    public int saves() {
        return saves;
    }

    private static QuestionReview copy(QuestionReview review, long version) {
        List<ReviewRound> rounds = review.rounds().stream()
                .map(r -> ReviewRound.restore(r.number(), r.submission(), r.status(), r.assignments(),
                        r.evaluation().orElse(null)))
                .toList();
        return QuestionReview.restore(review.questionId(), review.authorLogin(), rounds, version);
    }
}
