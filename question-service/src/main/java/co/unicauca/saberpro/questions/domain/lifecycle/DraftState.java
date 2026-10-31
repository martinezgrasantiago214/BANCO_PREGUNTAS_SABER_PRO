package co.unicauca.saberpro.questions.domain.lifecycle;

/** Borrador: el autor puede editarla, eliminarla o enviarla a revision. */
public final class DraftState implements QuestionState {

    public static final DraftState INSTANCE = new DraftState();

    private DraftState() {
    }

    @Override
    public QuestionStateType type() {
        return QuestionStateType.BORRADOR;
    }

    @Override
    public QuestionState submitForReview() {
        return PendingReviewState.INSTANCE;
    }

    @Override
    public QuestionState edit() {
        return this;
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
