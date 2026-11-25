package co.unicauca.saberpro.review.application.port.in;

import co.unicauca.saberpro.review.domain.model.QuestionSubmission;

/**
 * Puerto de entrada (HU02 -> HU04, HU06): una version de una pregunta llego a
 * revision. Lo invoca el adaptador de mensajeria al recibir question.submitted.
 */
public interface RegisterSubmissionUseCase {

    /** @return true si se abrio una revision o una ronda nueva; false si el envio ya se habia registrado. */
    boolean register(QuestionSubmission submission);
}
