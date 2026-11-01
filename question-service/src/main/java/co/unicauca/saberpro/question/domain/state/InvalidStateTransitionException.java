package co.unicauca.saberpro.question.domain.state;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.web.ConflictException;

/**
 * Se intento una accion que el estado actual de la pregunta no permite
 * (PSP-20). Responde 409 Conflict: la solicitud es valida, pero choca con el
 * estado actual del recurso. Como ConflictException extiende
 * BusinessException, el codigo que ya la atrapaba sigue funcionando.
 */
public class InvalidStateTransitionException extends ConflictException {

    public InvalidStateTransitionException(QuestionStatus current, String action) {
        super("No se puede " + action + " una pregunta en estado " + current.getLabel() + ".");
    }
}
