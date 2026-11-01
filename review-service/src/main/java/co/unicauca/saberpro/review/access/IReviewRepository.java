package co.unicauca.saberpro.review.access;

import co.unicauca.saberpro.review.domain.Evaluation;
import co.unicauca.saberpro.review.domain.PendingQuestion;
import co.unicauca.saberpro.review.domain.ReviewAssignment;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia del review-service (DIP). */
public interface IReviewRepository {

    void savePendingQuestion(PendingQuestion question);

    Optional<PendingQuestion> findPendingQuestion(String questionId);

    List<PendingQuestion> findPendingQuestions();

    void saveAssignment(ReviewAssignment assignment);

    List<ReviewAssignment> findAssignmentsByQuestion(String questionId);

    List<ReviewAssignment> findAssignmentsByReviewer(String reviewerLogin);

    void saveEvaluation(Evaluation evaluation);

    List<Evaluation> findEvaluationsByQuestion(String questionId);
}
