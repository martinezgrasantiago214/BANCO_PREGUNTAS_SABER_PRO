package co.unicauca.saberpro.reviews.domain.model;

import co.unicauca.saberpro.reviews.domain.exception.ReviewDomainException;
import co.unicauca.saberpro.reviews.domain.exception.ReviewDomainException.Kind;
import co.unicauca.saberpro.reviews.domain.policy.DecisionPolicy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Agregado raiz del contexto Revision por Pares. Guarda una copia local
 * (proyeccion) de los datos de la pregunta que llegaron en el evento
 * QuestionSubmittedForReview, de modo que este servicio no depende de
 * question-service para operar (autonomia de los microservicios).
 */
public class QuestionReview {

    public static final int MIN_REVIEWERS = 1;
    public static final int MAX_REVIEWERS = 3;

    private final String questionId;
    private final String authorLogin;
    private String statement;
    private String competence;
    private String topic;
    private ReviewStatus status;
    private int round;
    private Instant submittedAt;
    private String assignedBy;
    private Instant assignedAt;
    private Verdict finalDecision;
    private final List<ReviewerAssignment> assignments;
    private final List<Observation> observations;
    /** Version para control de concurrencia optimista (null = aun no persistida). */
    private Long version;

    public QuestionReview(String questionId, String authorLogin, String statement, String competence, String topic,
                          ReviewStatus status, int round, Instant submittedAt, String assignedBy, Instant assignedAt,
                          Verdict finalDecision, List<ReviewerAssignment> assignments, List<Observation> observations) {
        this.questionId = questionId;
        this.authorLogin = authorLogin;
        this.statement = statement;
        this.competence = competence;
        this.topic = topic;
        this.status = status;
        this.round = round;
        this.submittedAt = submittedAt;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
        this.finalDecision = finalDecision;
        this.assignments = new ArrayList<>(assignments);
        this.observations = new ArrayList<>(observations);
    }

    /** Crea la revision cuando llega una pregunta "Pendiente de revision" (HU02 -> HU04). */
    public static QuestionReview fromSubmission(String questionId, String authorLogin, String statement,
                                                String competence, String topic) {
        return new QuestionReview(questionId, authorLogin, statement, competence, topic,
                ReviewStatus.PENDIENTE_ASIGNACION, 1, Instant.now(), null, null, null, List.of(), List.of());
    }

    /**
     * Una pregunta rechazada, corregida y reenviada inicia una nueva ronda.
     * @return true si el reenvio abrio una ronda nueva; false si era un evento repetido.
     */
    public boolean resubmit(String statement, String competence, String topic) {
        if (status != ReviewStatus.RECHAZADA) {
            return false;
        }
        this.statement = statement;
        this.competence = competence;
        this.topic = topic;
        this.status = ReviewStatus.PENDIENTE_ASIGNACION;
        this.round++;
        this.submittedAt = Instant.now();
        this.assignedBy = null;
        this.assignedAt = null;
        this.finalDecision = null;
        this.assignments.clear();
        return true;
    }

    /** HU04: el administrador asigna entre 1 y 3 revisores distintos del autor. */
    public void assignReviewers(List<String> reviewerLogins, String adminLogin) {
        if (status != ReviewStatus.PENDIENTE_ASIGNACION) {
            throw new ReviewDomainException(Kind.INVALID_STATE,
                    "Solo se asignan revisores a preguntas Pendientes de revisión (estado actual: "
                            + status.getLabel() + ").");
        }
        Set<String> unique = new LinkedHashSet<>();
        if (reviewerLogins != null) {
            reviewerLogins.stream().filter(l -> l != null && !l.isBlank()).map(String::trim).forEach(unique::add);
        }
        if (unique.size() < MIN_REVIEWERS || unique.size() > MAX_REVIEWERS) {
            throw new ReviewDomainException(Kind.VALIDATION,
                    "Debe asignar entre " + MIN_REVIEWERS + " y " + MAX_REVIEWERS + " revisores distintos.");
        }
        if (unique.contains(authorLogin)) {
            throw new ReviewDomainException(Kind.VALIDATION, "El autor no puede revisar su propia pregunta.");
        }
        Instant now = Instant.now();
        unique.forEach(login -> assignments.add(new ReviewerAssignment(login, now)));
        this.assignedBy = adminLogin;
        this.assignedAt = now;
        this.status = ReviewStatus.EN_REVISION;
    }

    /**
     * HU05: el revisor registra su evaluacion. La politica (Strategy) decide
     * si con este concepto ya hay decision final.
     * @return la decision final si se alcanzo con esta evaluacion.
     */
    public Optional<Verdict> submitEvaluation(String reviewerLogin, Verdict verdict, List<CriterionScore> scores,
                                              String observationText, DecisionPolicy policy) {
        ensureInReview();
        ReviewerAssignment assignment = assignmentOf(reviewerLogin);
        assignment.complete(verdict, scores);
        if (observationText != null && !observationText.isBlank()) {
            observations.add(new Observation(reviewerLogin, observationText, null));
        }
        Optional<Verdict> decision = policy.decide(assignments);
        decision.ifPresent(this::close);
        return decision;
    }

    /** HU05: observacion independiente de la evaluacion, para el autor. */
    public Observation addObservation(String reviewerLogin, String text) {
        ensureInReview();
        assignmentOf(reviewerLogin);
        Observation observation = new Observation(reviewerLogin, text, null);
        observations.add(observation);
        return observation;
    }

    public boolean isAssignedTo(String reviewerLogin) {
        return assignments.stream().anyMatch(a -> a.getReviewerLogin().equals(reviewerLogin));
    }

    public List<String> reviewerLogins() {
        return assignments.stream().map(ReviewerAssignment::getReviewerLogin).toList();
    }

    private void close(Verdict decision) {
        this.finalDecision = decision;
        this.status = decision == Verdict.APROBADA ? ReviewStatus.APROBADA : ReviewStatus.RECHAZADA;
    }

    private void ensureInReview() {
        if (status != ReviewStatus.EN_REVISION) {
            throw new ReviewDomainException(Kind.INVALID_STATE,
                    "La pregunta no está en revisión (estado actual: " + status.getLabel() + ").");
        }
    }

    private ReviewerAssignment assignmentOf(String reviewerLogin) {
        return assignments.stream()
                .filter(a -> a.getReviewerLogin().equals(reviewerLogin))
                .findFirst()
                .orElseThrow(() -> new ReviewDomainException(Kind.FORBIDDEN,
                        "El usuario " + reviewerLogin + " no está asignado como revisor de esta pregunta."));
    }

    public String getQuestionId() { return questionId; }
    public String getAuthorLogin() { return authorLogin; }
    public String getStatement() { return statement; }
    public String getCompetence() { return competence; }
    public String getTopic() { return topic; }
    public ReviewStatus getStatus() { return status; }
    public int getRound() { return round; }
    public Instant getSubmittedAt() { return submittedAt; }
    public String getAssignedBy() { return assignedBy; }
    public Instant getAssignedAt() { return assignedAt; }
    public Verdict getFinalDecision() { return finalDecision; }
    public List<ReviewerAssignment> getAssignments() { return Collections.unmodifiableList(assignments); }
    public List<Observation> getObservations() { return Collections.unmodifiableList(observations); }
    public Long getVersion() { return version; }
    public QuestionReview withVersion(Long version) { this.version = version; return this; }
}
