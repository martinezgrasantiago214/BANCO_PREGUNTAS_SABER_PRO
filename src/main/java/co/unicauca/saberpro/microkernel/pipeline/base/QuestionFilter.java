package co.unicauca.saberpro.microkernel.pipeline.base;

import co.unicauca.saberpro.domain.QuestionRequest;

/**
 * Abstraccion del patron Tuberias y Filtros. Cada filtro concreto valida
 * una regla especifica sobre la solicitud de pregunta (HU03: validacion
 * estructural al momento de grabar).
 */
public interface QuestionFilter {

    /** @return true si la solicitud cumple la regla de este filtro. */
    boolean process(QuestionRequest request);

    /** Mensaje de error a mostrar cuando process(...) devuelve false. */
    String getErrorMessage();
}
