package co.unicauca.saberpro.review.domain;

import java.time.Instant;

/**
 * Copia local (solo lectura) de una pregunta que quedo Pendiente de revision.
 * El review-service la arma a partir del evento QuestionSubmittedForReviewEvent
 * para no consultar la base de datos del question-service (cada servicio es
 * dueno de sus datos). El administrador la ve en la lista de HU04.
 */
public record PendingQuestion(String questionId, String authorLogin, String directQuestion, Instant submittedAt) {
}
