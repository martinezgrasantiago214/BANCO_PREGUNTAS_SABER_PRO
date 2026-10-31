package co.unicauca.saberpro.reviews.domain.model;

import co.unicauca.saberpro.reviews.domain.exception.ReviewDomainException;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Asignacion de un revisor a una pregunta (entidad "Revision" del diagrama). */
public class ReviewerAssignment {

    private final String reviewerLogin;
    private final Instant assignedAt;
    private AssignmentStatus status;
    private Verdict verdict;
    private List<CriterionScore> scores;
    private Instant completedAt;

    public ReviewerAssignment(String reviewerLogin, Instant assignedAt) {
        this(reviewerLogin, assignedAt, AssignmentStatus.ASIGNADA, null, List.of(), null);
    }

    public ReviewerAssignment(String reviewerLogin, Instant assignedAt, AssignmentStatus status,
                              Verdict verdict, List<CriterionScore> scores, Instant completedAt) {
        this.reviewerLogin = reviewerLogin;
        this.assignedAt = assignedAt;
        this.status = status;
        this.verdict = verdict;
        this.scores = List.copyOf(scores);
        this.completedAt = completedAt;
    }

    /** Registra la evaluacion: exige calificar TODOS los criterios de la rubrica una sola vez. */
    void complete(Verdict verdict, List<CriterionScore> scores) {
        if (status == AssignmentStatus.COMPLETADA) {
            throw new ReviewDomainException(ReviewDomainException.Kind.INVALID_STATE,
                    "El revisor " + reviewerLogin + " ya registró su evaluación.");
        }
        if (verdict == null) {
            throw new ReviewDomainException(ReviewDomainException.Kind.VALIDATION,
                    "Debe indicar si la pregunta queda Aprobada o Rechazada.");
        }
        Set<ReviewCriterion> covered = EnumSet.noneOf(ReviewCriterion.class);
        for (CriterionScore s : scores) {
            if (!covered.add(s.criterion())) {
                throw new ReviewDomainException(ReviewDomainException.Kind.VALIDATION,
                        "El criterio " + s.criterion().getLabel() + " está repetido.");
            }
        }
        if (covered.size() != ReviewCriterion.values().length) {
            throw new ReviewDomainException(ReviewDomainException.Kind.VALIDATION,
                    "Debe calificar todos los criterios de la rúbrica.");
        }
        this.verdict = verdict;
        this.scores = List.copyOf(scores);
        this.status = AssignmentStatus.COMPLETADA;
        this.completedAt = Instant.now();
    }

    public double averageScore() {
        return scores.stream().mapToInt(CriterionScore::score).average().orElse(0);
    }

    public boolean isCompleted() {
        return status == AssignmentStatus.COMPLETADA;
    }

    public String getReviewerLogin() { return reviewerLogin; }
    public Instant getAssignedAt() { return assignedAt; }
    public AssignmentStatus getStatus() { return status; }
    public Verdict getVerdict() { return verdict; }
    public List<CriterionScore> getScores() { return scores; }
    public Instant getCompletedAt() { return completedAt; }
}
