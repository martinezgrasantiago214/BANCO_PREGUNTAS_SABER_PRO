package co.unicauca.saberpro.question.domain.factory;

import co.unicauca.saberpro.common.web.BusinessException;

/** La solicitud no supero la validacion estructural (responde 400 con el motivo). */
public class QuestionValidationException extends BusinessException {

    public QuestionValidationException(String reason) {
        super("La pregunta no supero la validacion estructural: " + reason);
    }
}
