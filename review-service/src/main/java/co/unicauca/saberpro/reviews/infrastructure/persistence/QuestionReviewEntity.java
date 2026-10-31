package co.unicauca.saberpro.reviews.infrastructure.persistence;

import co.unicauca.saberpro.reviews.domain.model.ReviewStatus;
import co.unicauca.saberpro.reviews.domain.model.Verdict;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Proceso de revision de una pregunta (proyeccion local + asignaciones + observaciones). */
@Entity
@Table(name = "question_reviews", indexes = @Index(name = "idx_review_status", columnList = "status"))
public class QuestionReviewEntity {

    @Id
    @Column(length = 36)
    private String questionId;

    @Column(nullable = false, length = 60)
    private String authorLogin;

    @Column(nullable = false, length = 2000)
    private String statement;

    @Column(length = 200)
    private String competence;

    @Column(length = 200)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewStatus status;

    @Column(nullable = false)
    private int round;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(length = 60)
    private String assignedBy;

    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Verdict finalDecision;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("assignedAt ASC, id ASC")
    private List<ReviewerAssignmentEntity> assignments = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "review_observations", joinColumns = @JoinColumn(name = "question_id"))
    @OrderBy("createdAt ASC")
    private List<ObservationEmbeddable> observations = new ArrayList<>();

    /** Cambia en cada guardado para que la version optimista siempre se incremente. */
    @Column(nullable = false)
    private Instant lastModifiedAt;

    @Version
    private Long version;

    protected QuestionReviewEntity() {
    }

    public QuestionReviewEntity(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionId() { return questionId; }
    public String getAuthorLogin() { return authorLogin; }
    public void setAuthorLogin(String authorLogin) { this.authorLogin = authorLogin; }
    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }
    public String getCompetence() { return competence; }
    public void setCompetence(String competence) { this.competence = competence; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }
    public int getRound() { return round; }
    public void setRound(int round) { this.round = round; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }
    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
    public Verdict getFinalDecision() { return finalDecision; }
    public void setFinalDecision(Verdict finalDecision) { this.finalDecision = finalDecision; }
    public List<ReviewerAssignmentEntity> getAssignments() { return assignments; }
    public List<ObservationEmbeddable> getObservations() { return observations; }
    public Long getVersion() { return version; }
    public void touch() { this.lastModifiedAt = Instant.now(); }
}
