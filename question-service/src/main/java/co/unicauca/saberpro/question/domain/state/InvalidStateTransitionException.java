package co.unicauca.saberpro.question.domain.state;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.web.BusinessException;

/** Se intento una accion que el estado actual de la pregunta no permite (responde 400). */
public class InvalidStateTransitionException extends BusinessException {

    public InvalidStateTransitionException(QuestionStatus current, String action) {
        super("No se puede " + action + " una pregunta en estado " + current.getLabel() + ".");
    }
}
