package co.unicauca.saberpro.question.domain.state;

import co.unicauca.saberpro.common.contract.QuestionStatus;

/** Eliminacion logica: la pregunta se conserva para trazabilidad, pero ya no admite acciones. */
public class EliminadaState extends AbstractQuestionState {

    @Override
    public QuestionStatus status() {
        return QuestionStatus.ELIMINADA;
    }
}
