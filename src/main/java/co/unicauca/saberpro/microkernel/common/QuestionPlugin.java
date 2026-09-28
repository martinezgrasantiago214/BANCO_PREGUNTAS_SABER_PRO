package co.unicauca.saberpro.microkernel.common;

import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionRequest;

/**
 * Contrato comun que deben cumplir todos los plugins del microkernel
 * (patron Microkernel: el nucleo solo conoce esta abstraccion, DIP).
 */
public interface QuestionPlugin {

    String getName();

    boolean supports(String type);

    /**
     * Ejecuta el pipeline de validacion propio del plugin (Tuberias y
     * Filtros) y, si la solicitud es valida, construye la Question.
     * Devuelve null si la solicitud no paso alguna validacion.
     */
    Question generate(QuestionRequest request);

    /** Ultimo mensaje de error de validacion producido por el pipeline del plugin. */
    String getLastValidationError();
}
