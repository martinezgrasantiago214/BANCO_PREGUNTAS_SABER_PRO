package co.unicauca.saberpro.questions.domain.lifecycle;

/** Aprobada: estado final; la pregunta ya puede usarse en simulacros. */
public final class ApprovedState implements QuestionState {

    public static final ApprovedState INSTANCE = new ApprovedState();

    private ApprovedState() {
    }

    @Override
    public QuestionStateType type() {
        return QuestionStateType.APROBADA;
    }
}
