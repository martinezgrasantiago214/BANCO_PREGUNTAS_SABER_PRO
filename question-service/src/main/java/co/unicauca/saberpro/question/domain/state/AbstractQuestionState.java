package co.unicauca.saberpro.question.domain.state;

/**
 * Estado base: por defecto ninguna transicion esta permitida y la pregunta
 * no es editable. Cada estado concreto sobrescribe solo lo que permite.
 */
abstract class AbstractQuestionState implements QuestionState {

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public QuestionState submitForReview() {
        throw denied("enviar a revision");
    }

    @Override
    public QuestionState approve() {
        throw denied("aprobar");
    }

    @Override
    public QuestionState reject() {
        throw denied("rechazar");
    }

    @Override
    public QuestionState delete() {
        throw denied("eliminar");
    }

    private InvalidStateTransitionException denied(String action) {
        return new InvalidStateTransitionException(status(), action);
    }
}
