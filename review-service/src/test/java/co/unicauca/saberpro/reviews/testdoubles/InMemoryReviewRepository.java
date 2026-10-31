package co.unicauca.saberpro.reviews.testdoubles;

import co.unicauca.saberpro.reviews.application.PageResult;
import co.unicauca.saberpro.reviews.application.port.ReviewRepositoryPort;
import co.unicauca.saberpro.reviews.domain.model.AssignmentStatus;
import co.unicauca.saberpro.reviews.domain.model.QuestionReview;
import co.unicauca.saberpro.reviews.domain.model.ReviewStatus;

import java.util.*;

public class InMemoryReviewRepository implements ReviewRepositoryPort {

    private final Map<String, QuestionReview> data = new LinkedHashMap<>();
    public int saves;

    @Override
    public QuestionReview save(QuestionReview review) {
        saves++;
        data.put(review.getQuestionId(), review);
        return review;
    }

    @Override
    public Optional<QuestionReview> findByQuestionId(String questionId) {
        return Optional.ofNullable(data.get(questionId));
    }

    @Override
    public PageResult<QuestionReview> findPendingAssignment(int page, int size) {
        return page(data.values().stream().filter(r -> r.getStatus() == ReviewStatus.PENDIENTE_ASIGNACION).toList(),
                page, size);
    }

    @Override
    public PageResult<QuestionReview> findAssignedTo(String reviewer, AssignmentStatus status, int page, int size) {
        return page(data.values().stream()
                .filter(r -> r.getAssignments().stream().anyMatch(a -> a.getReviewerLogin().equals(reviewer)
                        && (status == null || a.getStatus() == status)))
                .toList(), page, size);
    }

    private static PageResult<QuestionReview> page(List<QuestionReview> all, int page, int size) {
        int from = Math.min((page - 1) * size, all.size());
        return new PageResult<>(all.subList(from, Math.min(from + size, all.size())), page, size, all.size());
    }
}
