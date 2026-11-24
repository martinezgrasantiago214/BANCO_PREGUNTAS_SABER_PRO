package co.unicauca.saberpro.review.domain.model;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;

/**
 * Estado de una ronda de revision. Antes (segundo corte) el estado no
 * existia como tal: el servicio de aplicacion lo deducia preguntando si la
 * pregunta ya tenia evaluaciones. Ahora es parte del modelo y cada estado
 * responde que operaciones admite, de modo que las reglas no quedan
 * repartidas en "if" por los servicios.
 *
 * <pre>
 *   PENDIENTE_ASIGNACION --asignar--> EN_REVISION --evaluar--> APROBADA | RECHAZADA
 *                                     (asignar mas, hasta 3)
 *   RECHAZADA --version mejorada (HU06)--> nueva ronda en PENDIENTE_ASIGNACION
 * </pre>
 */
public enum ReviewStatus {
    PENDIENTE_ASIGNACION("Pendiente de asignacion"),
    EN_REVISION("En revision"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada");

    private final String label;

    ReviewStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** HU04: se pueden asignar revisores mientras no haya veredicto. */
    public boolean acceptsAssignments() {
        return this == PENDIENTE_ASIGNACION || this == EN_REVISION;
    }

    /** HU05: solo se evalua una pregunta que ya tiene revisores asignados y aun no tiene veredicto. */
    public boolean acceptsEvaluation() {
        return this == EN_REVISION;
    }

    /** La ronda termino con un veredicto. */
    public boolean isDecided() {
        return this == APROBADA || this == RECHAZADA;
    }

    /** HU06: solo una ronda rechazada da paso a una version mejorada. */
    public boolean allowsNewVersion() {
        return this == RECHAZADA;
    }

    public static ReviewStatus decidedBy(EvaluationVerdict verdict) {
        return verdict == EvaluationVerdict.APROBADA ? APROBADA : RECHAZADA;
    }
}
