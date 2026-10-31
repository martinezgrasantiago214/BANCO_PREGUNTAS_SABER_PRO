package co.unicauca.saberpro.questions.domain.model;

/** Nivel de dificultad segun el Diseno Centrado en Evidencia. */
public enum DifficultyLevel {
    BAJO("Bajo"),
    MEDIO("Medio"),
    ALTO("Alto");

    private final String label;

    DifficultyLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
