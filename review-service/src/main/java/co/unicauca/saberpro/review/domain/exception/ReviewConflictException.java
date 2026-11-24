package co.unicauca.saberpro.review.domain.exception;

import java.util.List;

/**
 * La operacion es valida, pero no en el estado actual de la revision (p. ej.
 * evaluar una pregunta que ya fue evaluada, o asignar revisores a una
 * pregunta ya decidida).
 */
public class ReviewConflictException extends ReviewDomainException {

    public ReviewConflictException(String message) {
        super(List.of(message));
    }
}
