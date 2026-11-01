package co.unicauca.saberpro.review.domain;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de negocio de HU05 sobre el contenido de una evaluacion.
 *
 * - El revisor debe elegir un veredicto: Aprobada o Rechazada.
 * - Al rechazar debe dejar observaciones, para que el autor sepa que corregir.
 * - Las observaciones tienen un tope de longitud.
 *
 * Las reglas sobre quien puede evaluar y cuando (pregunta asignada al
 * revisor, Pendiente de revision y aun sin veredicto) dependen de los datos
 * guardados y las aplica ReviewApplicationService.
 */
public final class EvaluationPolicy {

    public static final int MAX_OBSERVATIONS_LENGTH = 2000;

    private EvaluationPolicy() {
    }

    /** @return la lista de errores; vacia si la evaluacion es valida. */
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
}
