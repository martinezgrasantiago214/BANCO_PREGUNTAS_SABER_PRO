package co.unicauca.saberpro.microkernel.pipeline.filters;

import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionFilter;

/**
 * Valida que el contexto y la pregunta directa no esten vacios y cumplan
 * una longitud minima razonable.
 */
public class ContentValidationFilter implements QuestionFilter {

    private static final int MIN_LENGTH = 10;

    @Override
    public boolean process(QuestionRequest request) {
        return request.getContext() != null && !request.getContext().trim().isEmpty()
                && request.getDirectQuestion() != null
                && request.getDirectQuestion().trim().length() >= MIN_LENGTH;
    }

    @Override
    public String getErrorMessage() {
        return "El contexto y la pregunta directa son obligatorios "
                + "(la pregunta directa debe tener al menos " + MIN_LENGTH + " caracteres).";
    }
}
