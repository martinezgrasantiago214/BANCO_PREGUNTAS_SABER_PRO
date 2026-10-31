package co.unicauca.saberpro.users.domain.exception;

import java.util.List;

/** Error de negocio del contexto de usuarios, con la causa tipificada. */
public class UserDomainException extends RuntimeException {

    public enum Kind { VALIDATION, DUPLICATED, NOT_FOUND, INVALID_CREDENTIALS }

    private final Kind kind;
    private final List<String> details;

    public UserDomainException(Kind kind, String message) {
        this(kind, message, List.of());
    }

    public UserDomainException(Kind kind, String message, List<String> details) {
        super(message);
        this.kind = kind;
        this.details = List.copyOf(details);
    }

    public Kind getKind() { return kind; }
    public List<String> getDetails() { return details; }
}
