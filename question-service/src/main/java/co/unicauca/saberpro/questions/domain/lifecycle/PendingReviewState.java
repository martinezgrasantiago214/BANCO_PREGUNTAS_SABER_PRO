package co.unicauca.saberpro.questions.domain.lifecycle;

/** Pendiente de revision: espera que el administrador asigne revisores (HU04). */
public final class PendingReviewState implements QuestionState {

    public static final PendingReviewState INSTANCE = new PendingReviewState();

    private PendingReviewState() {
    }

    @Override
    public QuestionStateType type() {
        return QuestionStateType.PENDIENTE_REVISION;
    }

    @Override
    public QuestionState startReview() {
        return InReviewState.INSTANCE;
    }
}
