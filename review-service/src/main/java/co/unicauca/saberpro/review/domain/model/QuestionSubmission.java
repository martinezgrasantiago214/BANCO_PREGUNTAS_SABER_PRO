package co.unicauca.saberpro.review.domain.model;

import co.unicauca.saberpro.review.domain.exception.ReviewRuleViolationException;

import java.time.Instant;

/**
 * Objeto de valor: una version de una pregunta que el autor envio a
 * revision. Es la copia local que el review-service guarda de la pregunta
 * (no lee la base de datos del question-service).
 */
public record QuestionSubmission(String questionId, int questionVersion, String authorLogin,
                                 String directQuestion, Instant submittedAt) {

    public QuestionSubmission {
        if (questionId == null || questionId.isBlank()) {
            throw new ReviewRuleViolationException("El envio a revision no trae el identificador de la pregunta.");
        }
        questionId = questionId.trim();
        questionVersion = Math.max(questionVersion, 1);
        authorLogin = authorLogin != null ? authorLogin.trim() : null;
        submittedAt = submittedAt != null ? submittedAt : Instant.now();
    }
}
