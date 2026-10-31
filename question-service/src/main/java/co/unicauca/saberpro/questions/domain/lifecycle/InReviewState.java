package co.unicauca.saberpro.questions.domain.lifecycle;

/** En revision: los revisores asignados deciden si se aprueba o rechaza (HU05). */
public final class InReviewState implements QuestionState {

    public static final InReviewState INSTANCE = new InReviewState();

    private InReviewState() {
    }

    @Override
    public QuestionStateType type() {
        return QuestionStateType.EN_REVISION;
    }

    @Override
    public QuestionState approve() {
        return ApprovedState.INSTANCE;
    }

    @Override
    public QuestionState reject() {
        return RejectedState.INSTANCE;
    }
}
