package co.unicauca.saberpro.review.domain;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * HU05: evaluacion que deja un revisor sobre una pregunta asignada: el
 * veredicto (Aprobada/Rechazada) y las observaciones que leera el autor.
 */
public class Evaluation {

    private final String id;
    private final String questionId;
    private final String reviewerLogin;
    private final EvaluationVerdict verdict;
    private final String observations;
    private final Instant evaluatedAt;

    public Evaluation(String questionId, String reviewerLogin, EvaluationVerdict verdict, String observations) {
        this(UUID.randomUUID().toString(), questionId, reviewerLogin, verdict, observations, Instant.now());
    }

    public Evaluation(String id, String questionId, String reviewerLogin, EvaluationVerdict verdict,
                      String observations, Instant evaluatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.questionId = Objects.requireNonNull(questionId, "questionId");
        this.reviewerLogin = Objects.requireNonNull(reviewerLogin, "reviewerLogin");
        this.verdict = Objects.requireNonNull(verdict, "verdict");
        this.observations = observations != null ? observations.trim() : "";
        this.evaluatedAt = evaluatedAt;
    }

    public String getId() { return id; }
    public String getQuestionId() { return questionId; }
    public String getReviewerLogin() { return reviewerLogin; }
    public EvaluationVerdict getVerdict() { return verdict; }
    public String getObservations() { return observations; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
}
