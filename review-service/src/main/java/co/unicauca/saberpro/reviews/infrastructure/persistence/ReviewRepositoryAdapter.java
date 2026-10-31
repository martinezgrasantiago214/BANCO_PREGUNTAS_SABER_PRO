package co.unicauca.saberpro.reviews.infrastructure.persistence;

import co.unicauca.saberpro.reviews.application.PageResult;
import co.unicauca.saberpro.reviews.application.port.ReviewRepositoryPort;
import co.unicauca.saberpro.reviews.domain.model.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Adaptador JPA del puerto de persistencia de revisiones. */
@Component
public class ReviewRepositoryAdapter implements ReviewRepositoryPort {

    private final QuestionReviewJpaRepository jpa;

    public ReviewRepositoryAdapter(QuestionReviewJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public QuestionReview save(QuestionReview r) {
        QuestionReviewEntity e = jpa.findById(r.getQuestionId())
                .orElseGet(() -> new QuestionReviewEntity(r.getQuestionId()));
        if (r.getVersion() != null && !r.getVersion().equals(e.getVersion())) {
            // Otro revisor (u otra instancia) guardo antes: se evita la "actualizacion perdida".
            throw new OptimisticLockingFailureException("La revisión " + r.getQuestionId() + " cambió mientras se editaba.");
        }
        e.touch();
        e.setAuthorLogin(r.getAuthorLogin());
        e.setStatement(r.getStatement());
        e.setCompetence(r.getCompetence());
        e.setTopic(r.getTopic());
        e.setStatus(r.getStatus());
        e.setRound(r.getRound());
        e.setSubmittedAt(r.getSubmittedAt());
        e.setAssignedBy(r.getAssignedBy());
        e.setAssignedAt(r.getAssignedAt());
        e.setFinalDecision(r.getFinalDecision());
        syncAssignments(r, e);
        e.getObservations().clear();
        r.getObservations().forEach(o -> e.getObservations()
                .add(new ObservationEmbeddable(o.reviewerLogin(), o.text(), o.createdAt())));
        return toDomain(jpa.saveAndFlush(e));
    }

    /** Actualiza en sitio las asignaciones existentes y agrega/elimina las que cambiaron. */
    private void syncAssignments(QuestionReview r, QuestionReviewEntity e) {
        Map<String, ReviewerAssignment> wanted = r.getAssignments().stream()
                .collect(Collectors.toMap(ReviewerAssignment::getReviewerLogin, Function.identity()));
        e.getAssignments().removeIf(a -> !wanted.containsKey(a.getReviewerLogin()));
        for (ReviewerAssignment a : r.getAssignments()) {
            Optional<ReviewerAssignmentEntity> current = e.getAssignments().stream()
                    .filter(x -> x.getReviewerLogin().equals(a.getReviewerLogin())).findFirst();
            if (current.isPresent()) {
                ReviewerAssignmentEntity x = current.get();
                x.setStatus(a.getStatus());
                x.setVerdict(a.getVerdict());
                x.setCompletedAt(a.getCompletedAt());
                x.getScores().clear();
                a.getScores().forEach(s -> x.getScores()
                        .add(new CriterionScoreEmbeddable(s.criterion(), s.score(), s.comment())));
            } else {
                e.getAssignments().add(new ReviewerAssignmentEntity(e, a.getReviewerLogin(), a.getAssignedAt(),
                        a.getStatus(), a.getVerdict(), a.getCompletedAt(),
                        a.getScores().stream()
                                .map(s -> new CriterionScoreEmbeddable(s.criterion(), s.score(), s.comment()))
                                .toList()));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestionReview> findByQuestionId(String questionId) {
        return jpa.findById(questionId).map(ReviewRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionReview> findPendingAssignment(int page, int size) {
        Page<QuestionReviewEntity> result = jpa.findByStatus(ReviewStatus.PENDIENTE_ASIGNACION,
                PageRequest.of(page - 1, size, Sort.by("submittedAt").ascending()));
        return toPage(result, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionReview> findAssignedTo(String reviewerLogin, AssignmentStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("assignedAt").descending());
        Page<QuestionReviewEntity> result = status == null
                ? jpa.findAssignedTo(reviewerLogin, pageable)
                : jpa.findAssignedToWithStatus(reviewerLogin, status, pageable);
        return toPage(result, page, size);
    }

    private PageResult<QuestionReview> toPage(Page<QuestionReviewEntity> p, int page, int size) {
        return new PageResult<>(p.getContent().stream().map(ReviewRepositoryAdapter::toDomain).toList(),
                page, size, p.getTotalElements());
    }

    static QuestionReview toDomain(QuestionReviewEntity e) {
        return new QuestionReview(e.getQuestionId(), e.getAuthorLogin(), e.getStatement(), e.getCompetence(),
                e.getTopic(), e.getStatus(), e.getRound(), e.getSubmittedAt(), e.getAssignedBy(), e.getAssignedAt(),
                e.getFinalDecision(),
                e.getAssignments().stream().map(a -> new ReviewerAssignment(a.getReviewerLogin(), a.getAssignedAt(),
                        a.getStatus(), a.getVerdict(),
                        a.getScores().stream()
                                .map(s -> new CriterionScore(s.getCriterion(), s.getScore(), s.getComment()))
                                .toList(),
                        a.getCompletedAt())).toList(),
                e.getObservations().stream()
                        .map(o -> new Observation(o.getReviewerLogin(), o.getText(), o.getCreatedAt())).toList())
                .withVersion(e.getVersion());
    }
}
