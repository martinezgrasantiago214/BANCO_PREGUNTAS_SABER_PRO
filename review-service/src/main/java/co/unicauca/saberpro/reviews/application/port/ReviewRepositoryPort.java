package co.unicauca.saberpro.reviews.application.port;

import co.unicauca.saberpro.reviews.application.PageResult;
import co.unicauca.saberpro.reviews.domain.model.AssignmentStatus;
import co.unicauca.saberpro.reviews.domain.model.QuestionReview;

import java.util.Optional;

public interface ReviewRepositoryPort {

    QuestionReview save(QuestionReview review);

    Optional<QuestionReview> findByQuestionId(String questionId);

    PageResult<QuestionReview> findPendingAssignment(int page, int size);

    /** @param status null = todas las asignaciones del revisor. */
    PageResult<QuestionReview> findAssignedTo(String reviewerLogin, AssignmentStatus status, int page, int size);
}
