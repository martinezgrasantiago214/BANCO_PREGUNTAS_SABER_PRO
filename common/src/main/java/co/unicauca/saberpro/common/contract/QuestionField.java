package co.unicauca.saberpro.common.contract;

/**
 * Campos de texto de una pregunta (HU01) con sus reglas de captura: nombre
 * visible, longitud minima y longitud maxima. Es la unica fuente de esas
 * reglas, de modo que el formulario del frontend (validacion en linea y
 * contadores) y la validacion estructural del question-service no se
 * contradigan (PSP-15).
 *
 * Los distractores, la respuesta correcta y el nivel de dificultad no son
 * texto libre y se validan aparte.
 */
public enum QuestionField {
    CONTEXT("Contexto", 1, 2000),
    DIRECT_QUESTION("Pregunta directa", 10, 500),
    JUSTIFICATION("Justificacion de la respuesta", 1, 2000),
    BIBLIOGRAPHY("Bibliografia", 1, 500),
    COMPETENCE("Competencia", 1, 100),
    TOPIC("Tema", 1, 100),
    SUBTOPIC("Subtema", 1, 100);

    private final String label;
    private final int minLength;
    private final int maxLength;

    QuestionField(String label, int minLength, int maxLength) {
        this.label = label;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    public String getLabel() { return label; }
    public int getMinLength() { return minLength; }
    public int getMaxLength() { return maxLength; }

    /**
     * Valida el valor capturado para este campo (sin espacios al inicio ni
     * al final).
     *
     * @return null si el valor es valido, o el mensaje de error a mostrar.
     */
    public String validate(String value) {
        int length = value == null ? 0 : value.trim().length();
        if (length == 0) {
            return "El campo " + label + " es obligatorio.";
        }
        if (length < minLength) {
            return "El campo " + label + " debe tener al menos " + minLength + " caracteres.";
        }
        if (length > maxLength) {
            return "El campo " + label + " no puede superar " + maxLength + " caracteres.";
        }
        return null;
    }

    @Override
    public String toString() {
        return label;
    }
}
