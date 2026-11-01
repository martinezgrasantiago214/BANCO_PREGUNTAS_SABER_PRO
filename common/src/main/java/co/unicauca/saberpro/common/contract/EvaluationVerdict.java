package co.unicauca.saberpro.common.contract;

/** Resultado de la evaluacion de un revisor sobre una pregunta (HU05). */
public enum EvaluationVerdict {
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada");

    private final String label;

    EvaluationVerdict(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
