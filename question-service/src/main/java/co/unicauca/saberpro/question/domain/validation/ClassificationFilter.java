package co.unicauca.saberpro.question.domain.validation;

import co.unicauca.saberpro.question.domain.QuestionRequest;

/**
 * Valida que la clasificacion de la pregunta (Competencia, Tema, Subtema y
 * Nivel de dificultad, HU01) este completa.
 */
public class ClassificationFilter implements QuestionFilter {

    @Override
    public boolean process(QuestionRequest request) {
        return notBlank(request.getCompetence())
                && notBlank(request.getTopic())
                && notBlank(request.getSubtopic())
                && request.getDifficultyLevel() != null;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public String getErrorMessage() {
        return "Debe indicar Competencia, Tema, Subtema y Nivel de dificultad.";
    }
}
