package co.unicauca.saberpro.reviews.infrastructure.persistence;

import co.unicauca.saberpro.reviews.domain.model.AssignmentStatus;
import co.unicauca.saberpro.reviews.domain.model.Verdict;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Tabla "revision": un revisor asignado a una pregunta. */
@Entity
@Table(name = "reviewer_assignments", indexes = @Index(name = "idx_assignment_reviewer", columnList = "reviewerLogin"))
public class ReviewerAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private QuestionReviewEntity review;

    @Column(nullable = false, length = 60)
    private String reviewerLogin;

    @Column(nullable = false)
    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Verdict verdict;

    private Instant completedAt;

    @ElementCollection
    @CollectionTable(name = "reviewer_scores", joinColumns = @JoinColumn(name = "assignment_id"))
    private List<CriterionScoreEmbeddable> scores = new ArrayList<>();

    protected ReviewerAssignmentEntity() {
    }

    public ReviewerAssignmentEntity(QuestionReviewEntity review, String reviewerLogin, Instant assignedAt,
                                    AssignmentStatus status, Verdict verdict, Instant completedAt,
                                    List<CriterionScoreEmbeddable> scores) {
        this.review = review;
        this.reviewerLogin = reviewerLogin;
        this.assignedAt = assignedAt;
        this.status = status;
        this.verdict = verdict;
        this.completedAt = completedAt;
        this.scores = new ArrayList<>(scores);
    }

    public Long getId() { return id; }
    public String getReviewerLogin() { return reviewerLogin; }
    public Instant getAssignedAt() { return assignedAt; }
    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }
    public Verdict getVerdict() { return verdict; }
    public void setVerdict(Verdict verdict) { this.verdict = verdict; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public List<CriterionScoreEmbeddable> getScores() { return scores; }
}
