package co.unicauca.saberpro.review.access;

import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.review.domain.Evaluation;
import co.unicauca.saberpro.review.domain.PendingQuestion;
import co.unicauca.saberpro.review.domain.ReviewAssignment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementacion en memoria para las pruebas unitarias: los datos se pierden
 * al reiniciar. El servicio usa {@link SQLiteReviewRepository} (PSP-33).
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
    public synchronized void saveAssignment(ReviewAssignment assignment) {
        boolean duplicate = assignments.stream()
                .anyMatch(a -> a.isFor(assignment.getQuestionId(), assignment.getReviewerLogin()));
        if (duplicate) {
            throw new BusinessException("El revisor " + assignment.getReviewerLogin()
                    + " ya esta asignado a la pregunta " + assignment.getQuestionId() + ".");
        }
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
