package co.unicauca.saberpro.question.domain.state;

import co.unicauca.saberpro.common.contract.QuestionStatus;

/** Estado inicial: el autor puede editar, enviar a revision o eliminar la pregunta. */
public class BorradorState extends AbstractQuestionState {

    @Override
    public QuestionStatus status() {
        return QuestionStatus.BORRADOR;
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public QuestionState submitForReview() {
        return new PendienteRevisionState();
    }

    @Override
    public QuestionState delete() {
        return new EliminadaState();
    }
}
