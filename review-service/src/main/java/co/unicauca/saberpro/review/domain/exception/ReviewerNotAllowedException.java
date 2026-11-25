package co.unicauca.saberpro.review.domain.exception;

import java.util.List;

/** El usuario no puede hacer esta operacion sobre la revision (p. ej. evaluar una pregunta que no le asignaron). */
public class ReviewerNotAllowedException extends ReviewDomainException {

    public ReviewerNotAllowedException(String message) {
        super(List.of(message));
    }
}
