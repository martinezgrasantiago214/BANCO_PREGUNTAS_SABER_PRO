package co.unicauca.saberpro.microkernel.pipeline.filters;

import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionFilter;

/**
 * Valida que la clasificacion de la pregunta (Competencia, Tema, Subtema y
 * Nivel de dificultad, HU01) este completa. Ejemplo de competencia:
 * "Arquitectura de software".
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
