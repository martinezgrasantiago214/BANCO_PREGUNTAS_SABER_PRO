package co.unicauca.saberpro.review.access;

import co.unicauca.saberpro.review.domain.Evaluation;
import co.unicauca.saberpro.review.domain.PendingQuestion;
import co.unicauca.saberpro.review.domain.ReviewAssignment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementacion temporal en memoria: los datos se pierden al reiniciar.
 *
 * TODO(PSP-33): reemplazarla por una base de datos propia del review-service
 * (por ejemplo SQLite, igual que SQLiteQuestionRepository en el
 * question-service) para que la asignacion quede registrada.
 */
public class InMemoryReviewRepository implements IReviewRepository {

    private final Map<String, PendingQuestion> pending = new ConcurrentHashMap<>();
    private final List<ReviewAssignment> assignments = new CopyOnWriteArrayList<>();
    private final List<Evaluation> evaluations = new CopyOnWriteArrayList<>();

    @Override
    public void savePendingQuestion(PendingQuestion question) {
        pending.put(question.questionId(), question);
    }

    @Override
    public Optional<PendingQuestion> findPendingQuestion(String questionId) {
        return Optional.ofNullable(pending.get(questionId));
    }

    @Override
    public List<PendingQuestion> findPendingQuestions() {
        return List.copyOf(pending.values());
    }

    @Override
    public void saveAssignment(ReviewAssignment assignment) {
        assignments.add(assignment);
    }

    @Override
    public List<ReviewAssignment> findAssignmentsByQuestion(String questionId) {
        return assignments.stream().filter(a -> a.getQuestionId().equals(questionId)).toList();
    }

    @Override
    public List<ReviewAssignment> findAssignmentsByReviewer(String reviewerLogin) {
        return assignments.stream().filter(a -> a.getReviewerLogin().equals(reviewerLogin)).toList();
    }

    @Override
    public void saveEvaluation(Evaluation evaluation) {
        evaluations.add(evaluation);
    }

    @Override
    public List<Evaluation> findEvaluationsByQuestion(String questionId) {
        return evaluations.stream().filter(e -> e.getQuestionId().equals(questionId)).toList();
    }
}
