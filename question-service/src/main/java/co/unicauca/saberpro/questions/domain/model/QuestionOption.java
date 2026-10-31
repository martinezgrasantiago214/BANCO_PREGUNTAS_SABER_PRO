package co.unicauca.saberpro.questions.domain.model;

/** Opcion de respuesta (entidad "Opcion" del diagrama de contextos). */
public record QuestionOption(String text, boolean correct, int order) {

    public QuestionOption {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("El texto de la opción es obligatorio.");
        }
        text = text.trim();
    }
}
