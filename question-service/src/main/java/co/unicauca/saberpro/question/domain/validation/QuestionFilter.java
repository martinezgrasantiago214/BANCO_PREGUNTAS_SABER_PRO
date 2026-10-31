package co.unicauca.saberpro.question.domain.validation;

import co.unicauca.saberpro.question.domain.QuestionRequest;

/**
 * Filtro del pipeline de validacion estructural (patron Tuberias y Filtros
 * del primer corte). Cada filtro valida una regla de la solicitud.
 */
public interface QuestionFilter {

    /** @return true si la solicitud cumple la regla de este filtro. */
    boolean process(QuestionRequest request);

    /** Mensaje de error a mostrar cuando process(...) devuelve false. */
    String getErrorMessage();
}
