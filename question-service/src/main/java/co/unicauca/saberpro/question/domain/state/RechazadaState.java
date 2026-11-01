package co.unicauca.saberpro.question.domain.state;

import co.unicauca.saberpro.common.contract.QuestionStatus;

/** La pregunta fue rechazada por un revisor (HU05); el autor lee las observaciones. */
public class RechazadaState extends AbstractQuestionState {

    @Override
    public QuestionStatus status() {
        return QuestionStatus.RECHAZADA;
    }
}
