package co.unicauca.saberpro.questions.domain.lifecycle;

/**
 * Rechazada: el autor puede corregirla atendiendo las observaciones; al
 * editarla vuelve a Borrador para iniciar de nuevo el ciclo.
 */
public final class RejectedState implements QuestionState {

    public static final RejectedState INSTANCE = new RejectedState();

    private RejectedState() {
    }

    @Override
    public QuestionStateType type() {
        return QuestionStateType.RECHAZADA;
    }

    @Override
    public QuestionState edit() {
        return DraftState.INSTANCE;
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public boolean isDeletable() {
        return true;
    }
}
