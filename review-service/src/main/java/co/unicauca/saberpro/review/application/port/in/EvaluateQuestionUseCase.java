package co.unicauca.saberpro.review.application.port.in;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.review.domain.model.Evaluation;

/** Puerto de entrada (HU05): un revisor aprueba o rechaza una pregunta asignada. */
public interface EvaluateQuestionUseCase {

    record EvaluateQuestionCommand(String questionId, String reviewerLogin, EvaluationVerdict verdict,
                                   String observations) {
    }

    Evaluation evaluate(EvaluateQuestionCommand command);
}
