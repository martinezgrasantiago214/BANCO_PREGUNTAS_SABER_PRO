package co.unicauca.saberpro.question.domain.validation;

import co.unicauca.saberpro.question.domain.QuestionRequest;

/**
 * Toda pregunta debe traer Justificacion de la respuesta y Bibliografia
 * (Diseno Centrado en Evidencia), ademas de un autor identificado.
 */
public class MetadataValidationFilter implements QuestionFilter {

    @Override
    public boolean process(QuestionRequest request) {
        return notBlank(request.getJustification())
                && notBlank(request.getBibliography())
                && notBlank(request.getAuthorLogin());
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public String getErrorMessage() {
        return "Debe indicar Justificacion de la respuesta, Bibliografia, y estar autenticado como autor.";
    }
}
