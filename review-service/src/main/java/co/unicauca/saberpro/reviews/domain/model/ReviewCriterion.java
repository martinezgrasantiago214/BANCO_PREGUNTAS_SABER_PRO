package co.unicauca.saberpro.reviews.domain.model;

/**
 * Rubrica de evaluacion del revisor, alineada con el Diseno Centrado en
 * Evidencia: cada criterio se califica de 1 a 5.
 */
public enum ReviewCriterion {
    ALINEACION_COMPETENCIA("Alineación con la competencia y la evidencia"),
    CLARIDAD_ENUNCIADO("Claridad del contexto y la pregunta directa"),
    CALIDAD_DISTRACTORES("Plausibilidad de los distractores"),
    JUSTIFICACION_BIBLIOGRAFIA("Justificación y bibliografía pertinentes");

    private final String label;

    ReviewCriterion(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
