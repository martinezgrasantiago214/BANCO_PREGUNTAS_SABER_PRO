package co.unicauca.saberpro.review.domain.model;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.review.domain.exception.ReviewRuleViolationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor (HU05): veredicto y observaciones de un revisor. Es
 * inmutable y solo se crea valido con {@link #issue}: las reglas de su
 * contenido estan aqui y no en el servicio de aplicacion.
 *
 * <ul>
 *   <li>El veredicto es obligatorio: Aprobada o Rechazada.</li>
 *   <li>Al rechazar hay que dejar observaciones, para que el autor sepa que corregir (HU06).</li>
 *   <li>Las observaciones tienen un tope de {@value #MAX_OBSERVATIONS_LENGTH} caracteres.</li>
 * </ul>
 */
public record Evaluation(String id, String reviewerLogin, EvaluationVerdict verdict, String observations,
                         Instant evaluatedAt) {

    public static final int MAX_OBSERVATIONS_LENGTH = 2000;

    public Evaluation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(reviewerLogin, "reviewerLogin");
        Objects.requireNonNull(verdict, "verdict");
        observations = observations == null ? "" : observations.trim();
        Objects.requireNonNull(evaluatedAt, "evaluatedAt");
    }

    /** Crea una evaluacion nueva validando su contenido; si algo falla, reporta todos los errores juntos. */
    public static Evaluation issue(String reviewerLogin, EvaluationVerdict verdict, String observations,
                                   Instant evaluatedAt) {
        List<String> errors = validate(verdict, observations);
        if (!errors.isEmpty()) {
            throw new ReviewRuleViolationException(errors);
        }
        return new Evaluation(UUID.randomUUID().toString(), reviewerLogin, verdict, observations, evaluatedAt);
    }

    /** @return los errores del contenido de la evaluacion; vacia si es valida. */
    public static List<String> validate(EvaluationVerdict verdict, String observations) {
        List<String> errors = new ArrayList<>();
        String text = observations == null ? "" : observations.trim();
        if (verdict == null) {
            errors.add("Debe indicar el veredicto: Aprobada o Rechazada.");
        } else if (verdict == EvaluationVerdict.RECHAZADA && text.isEmpty()) {
            errors.add("Al rechazar una pregunta debe dejar observaciones para el autor.");
        }
        if (text.length() > MAX_OBSERVATIONS_LENGTH) {
            errors.add("Las observaciones no pueden superar " + MAX_OBSERVATIONS_LENGTH + " caracteres.");
        }
        return errors;
    }

    public boolean isRejection() {
        return verdict == EvaluationVerdict.RECHAZADA;
    }
}
