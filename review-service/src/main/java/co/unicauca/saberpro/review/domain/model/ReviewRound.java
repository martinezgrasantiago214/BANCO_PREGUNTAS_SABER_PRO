package co.unicauca.saberpro.review.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Entidad dentro del agregado {@link QuestionReview}: una ronda de revision
 * de UNA version de la pregunta (HU06). La ronda 1 revisa la version 1; si
 * se rechaza y el autor saca una version mejorada, la ronda 2 revisa la
 * version 2, y asi. El conjunto de rondas es el historial de revisiones.
 *
 * Su estado solo cambia por los metodos de negocio del agregado (no hay
 * setters publicos): {@link #assign} y {@link #decide} son de paquete.
 */
public class ReviewRound {

    private final int number;
    private final QuestionSubmission submission;
    private final List<ReviewerAssignment> assignments;
    private Evaluation evaluation;
    private ReviewStatus status;

    private ReviewRound(int number, QuestionSubmission submission, ReviewStatus status,
                        List<ReviewerAssignment> assignments, Evaluation evaluation) {
        if (number < 1) {
            throw new IllegalArgumentException("El numero de ronda empieza en 1.");
        }
        this.number = number;
        this.submission = Objects.requireNonNull(submission, "submission");
        this.status = Objects.requireNonNull(status, "status");
        this.assignments = new ArrayList<>(assignments);
        this.evaluation = evaluation;
    }

    /** Ronda nueva, recien enviada: aun no tiene revisores. */
    static ReviewRound open(int number, QuestionSubmission submission) {
        return new ReviewRound(number, submission, ReviewStatus.PENDIENTE_ASIGNACION, List.of(), null);
    }

    /**
     * Reconstruye una ronda guardada (lo usa el adaptador de persistencia) y
     * verifica que los datos sean coherentes con su estado.
     */
    public static ReviewRound restore(int number, QuestionSubmission submission, ReviewStatus status,
                                      List<ReviewerAssignment> assignments, Evaluation evaluation) {
        List<ReviewerAssignment> list = assignments == null ? List.of() : assignments;
        if (status.isDecided() && evaluation == null) {
            throw new IllegalStateException("La ronda " + number + " esta " + status.getLabel()
                    + " pero no tiene evaluacion.");
        }
        if (status == ReviewStatus.PENDIENTE_ASIGNACION && !list.isEmpty()) {
            throw new IllegalStateException("La ronda " + number + " tiene revisores pero figura sin asignar.");
        }
        return new ReviewRound(number, submission, status, list, evaluation);
    }

    // ---- comportamiento (solo lo invoca el agregado) ----

    void assign(List<ReviewerAssignment> newAssignments) {
        assignments.addAll(newAssignments);
        status = ReviewStatus.EN_REVISION;
    }

    void decide(Evaluation decisive) {
        this.evaluation = decisive;
        this.status = ReviewStatus.decidedBy(decisive.verdict());
    }

    // ---- consultas ----

    public int number() {
        return number;
    }

    public QuestionSubmission submission() {
        return submission;
    }

    public int questionVersion() {
        return submission.questionVersion();
    }

    public String directQuestion() {
        return submission.directQuestion();
    }

    public Instant submittedAt() {
        return submission.submittedAt();
    }

    public ReviewStatus status() {
        return status;
    }

    public List<ReviewerAssignment> assignments() {
        return Collections.unmodifiableList(assignments);
    }

    public List<String> reviewerLogins() {
        return assignments.stream().map(ReviewerAssignment::reviewerLogin).toList();
    }

    public boolean isAssigned(String login) {
        return assignments.stream().anyMatch(a -> a.isFor(login));
    }

    public Optional<Evaluation> evaluation() {
        return Optional.ofNullable(evaluation);
    }

    public int remainingReviewerSlots() {
        return ReviewerSelection.MAX_REVIEWERS - assignments.size();
    }
}
