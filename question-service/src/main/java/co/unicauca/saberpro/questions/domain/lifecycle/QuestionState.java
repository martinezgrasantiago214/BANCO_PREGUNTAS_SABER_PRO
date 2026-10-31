package co.unicauca.saberpro.questions.domain.lifecycle;

import co.unicauca.saberpro.questions.domain.exception.QuestionDomainException;

/**
 * Patron State (GoF, comportamiento). Cada estado concreto decide que
 * transiciones son validas y devuelve el estado siguiente. La entidad
 * Question delega en su estado actual en vez de tener if/switch por estado,
 * de modo que agregar un estado nuevo (p. ej. "Archivada") no modifica la
 * entidad (OCP).
 *
 * Por defecto toda transicion es invalida; cada estado sobrescribe solo las
 * que permite.
 */
public interface QuestionState {

    QuestionStateType type();

    default QuestionState submitForReview() {
        throw invalid("enviar a revisión");
    }

    default QuestionState startReview() {
        throw invalid("iniciar la revisión");
    }

    default QuestionState approve() {
        throw invalid("aprobar");
    }

    default QuestionState reject() {
        throw invalid("rechazar");
    }

    /** Estado resultante de editar la pregunta. */
    default QuestionState edit() {
        throw invalid("editar");
    }

    default boolean isEditable() {
        return false;
    }

    default boolean isDeletable() {
        return false;
    }

    private QuestionDomainException invalid(String action) {
        return new QuestionDomainException(QuestionDomainException.Kind.INVALID_TRANSITION,
                "No se puede " + action + " una pregunta en estado \"" + type().getLabel() + "\".");
    }
}
