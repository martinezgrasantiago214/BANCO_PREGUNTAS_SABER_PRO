package co.unicauca.saberpro.review.domain.event;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;

import java.time.Instant;

/** HU05: un revisor decidio la ronda (Aprobada o Rechazada). */
public record QuestionEvaluated(String questionId, int questionVersion, int round, String authorLogin,
                                String directQuestion, String reviewerLogin, EvaluationVerdict verdict,
                                String observations, Instant occurredAt) implements ReviewDomainEvent {
}
