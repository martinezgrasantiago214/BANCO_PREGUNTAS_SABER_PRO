package co.unicauca.saberpro.question.domain.factory;

import co.unicauca.saberpro.common.web.BusinessException;

import java.util.List;

/** La solicitud no supero la validacion estructural (responde 400 con el motivo). */
public class QuestionValidationException extends BusinessException {

    public QuestionValidationException(String reason) {
        super("La pregunta no supero la validacion estructural: " + reason);
    }

    /** Todos los errores de la validacion estructural, uno por regla incumplida (400 con la lista). */
    public QuestionValidationException(List<String> reasons) {
        super(reasons);
    }
}
