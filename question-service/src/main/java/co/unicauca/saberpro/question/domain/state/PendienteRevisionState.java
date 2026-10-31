package co.unicauca.saberpro.question.domain.state;

import co.unicauca.saberpro.common.contract.QuestionStatus;

/**
 * La pregunta espera revision: el administrador le asigna revisores (HU04) y
 * un revisor la aprueba o la rechaza (HU05). Ya no se puede editar.
 */
public class PendienteRevisionState extends AbstractQuestionState {

    @Override
    public QuestionStatus status() {
        return QuestionStatus.PENDIENTE_REVISION;
    }

    @Override
    public QuestionState approve() {
        return new AprobadaState();
    }

    @Override
    public QuestionState reject() {
        return new RechazadaState();
    }
}
