package co.unicauca.saberpro.reviews.infrastructure.persistence;

import co.unicauca.saberpro.reviews.domain.model.AssignmentStatus;
import co.unicauca.saberpro.reviews.domain.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionReviewJpaRepository extends JpaRepository<QuestionReviewEntity, String> {

    Page<QuestionReviewEntity> findByStatus(ReviewStatus status, Pageable pageable);

    @Query(value = "select r from QuestionReviewEntity r where exists ("
            + " select a from ReviewerAssignmentEntity a where a.review = r and a.reviewerLogin = :login)",
           countQuery = "select count(r) from QuestionReviewEntity r where exists ("
            + " select a from ReviewerAssignmentEntity a where a.review = r and a.reviewerLogin = :login)")
    Page<QuestionReviewEntity> findAssignedTo(@Param("login") String login, Pageable pageable);

    @Query(value = "select r from QuestionReviewEntity r where exists ("
            + " select a from ReviewerAssignmentEntity a where a.review = r and a.reviewerLogin = :login"
            + " and a.status = :status)",
           countQuery = "select count(r) from QuestionReviewEntity r where exists ("
            + " select a from ReviewerAssignmentEntity a where a.review = r and a.reviewerLogin = :login"
            + " and a.status = :status)")
    Page<QuestionReviewEntity> findAssignedToWithStatus(@Param("login") String login,
                                                        @Param("status") AssignmentStatus status,
                                                        Pageable pageable);
}
