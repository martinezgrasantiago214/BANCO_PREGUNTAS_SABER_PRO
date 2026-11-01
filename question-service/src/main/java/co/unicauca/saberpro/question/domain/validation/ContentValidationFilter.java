package co.unicauca.saberpro.question.domain.validation;

import co.unicauca.saberpro.common.contract.QuestionField;
import co.unicauca.saberpro.question.domain.QuestionRequest;

/**
 * Valida que el contexto y la pregunta directa no esten vacios. El minimo de
 * la pregunta directa sale de {@link QuestionField}, la misma regla que usa
 * el formulario del frontend.
 */
public class ContentValidationFilter implements QuestionFilter {

    private static final int MIN_LENGTH = QuestionField.DIRECT_QUESTION.getMinLength();

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
