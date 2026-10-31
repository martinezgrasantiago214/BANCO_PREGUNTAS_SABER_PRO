package co.unicauca.saberpro.questions.domain.exception;

import java.util.List;

/** Error de negocio del contexto de preguntas, con su causa tipificada. */
public class QuestionDomainException extends RuntimeException {

    public enum Kind { VALIDATION, NOT_FOUND, INVALID_TRANSITION, FORBIDDEN }

    private final Kind kind;
    private final List<String> details;

    public QuestionDomainException(Kind kind, String message) {
        this(kind, message, List.of());
    }

    public QuestionDomainException(Kind kind, String message, List<String> details) {
        super(message);
        this.kind = kind;
        this.details = List.copyOf(details);
    }

    public Kind getKind() { return kind; }
    public List<String> getDetails() { return details; }

    public static QuestionDomainException notFound(String id) {
        return new QuestionDomainException(Kind.NOT_FOUND, "No existe una pregunta con id " + id + ".");
    }

    public static QuestionDomainException forbidden(String message) {
        return new QuestionDomainException(Kind.FORBIDDEN, message);
    }
}
