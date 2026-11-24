package co.unicauca.saberpro.review.domain.exception;

import java.util.List;

/** No hay una revision para esa pregunta (nunca se envio a revision). */
public class ReviewNotFoundException extends ReviewDomainException {

    public ReviewNotFoundException(String questionId) {
        super(List.of("La pregunta " + questionId + " no tiene revisiones: aun no se ha enviado a revision."));
    }
}
