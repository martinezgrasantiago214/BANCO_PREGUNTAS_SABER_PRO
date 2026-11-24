package co.unicauca.saberpro.review.domain.exception;

import java.util.List;

/** Se incumplio una regla de negocio de los datos enviados (p. ej. 4 revisores o rechazo sin observaciones). */
public class ReviewRuleViolationException extends ReviewDomainException {

    public ReviewRuleViolationException(String message) {
        this(List.of(message));
    }

    public ReviewRuleViolationException(List<String> messages) {
        super(messages);
    }
}
