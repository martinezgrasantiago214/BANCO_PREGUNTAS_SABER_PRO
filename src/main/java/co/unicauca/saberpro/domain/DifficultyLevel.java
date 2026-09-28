package co.unicauca.saberpro.domain;

/** Nivel de dificultad de una pregunta, segun el Diseno Centrado en Evidencia. */
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

    @Override
    public String toString() {
        return label;
    }
}
