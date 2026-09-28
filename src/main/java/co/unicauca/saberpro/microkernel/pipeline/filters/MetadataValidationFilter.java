package co.unicauca.saberpro.microkernel.pipeline.filters;

import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionFilter;

/**
 * Filtro adicional exigido por el Diseno Centrado en Evidencia (HU01/HU03):
 * toda pregunta debe traer Justificacion de la respuesta y Bibliografia,
 * ademas de un autor identificado.
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
