package co.unicauca.saberpro.question.domain.state;

import co.unicauca.saberpro.common.contract.QuestionStatus;

/** Estado final: la pregunta fue aprobada por un revisor (HU05). */
public class AprobadaState extends AbstractQuestionState {

    @Override
    public QuestionStatus status() {
        return QuestionStatus.APROBADA;
    }
}
