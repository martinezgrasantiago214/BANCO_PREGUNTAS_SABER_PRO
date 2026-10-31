package co.unicauca.saberpro.reviews.domain.exception;

import java.util.List;

/** Error de negocio del contexto de revision. */
public class ReviewDomainException extends RuntimeException {

    public enum Kind { VALIDATION, NOT_FOUND, INVALID_STATE, FORBIDDEN }

    private final Kind kind;
    private final List<String> details;

    public ReviewDomainException(Kind kind, String message) {
        this(kind, message, List.of());
    }

    public ReviewDomainException(Kind kind, String message, List<String> details) {
        super(message);
        this.kind = kind;
        this.details = List.copyOf(details);
    }

    public Kind getKind() { return kind; }
    public List<String> getDetails() { return details; }
}
