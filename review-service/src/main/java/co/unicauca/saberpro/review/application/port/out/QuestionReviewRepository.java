package co.unicauca.saberpro.review.application.port.out;

import co.unicauca.saberpro.review.domain.model.QuestionReview;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia del agregado completo (raiz + rondas +
 * asignaciones + evaluaciones). Se guarda y se lee siempre el agregado
 * entero, nunca una de sus partes por separado.
 */
public interface QuestionReviewRepository {

    Optional<QuestionReview> findById(String questionId);

    /**
     * Inserta o actualiza el agregado.
     *
     * @throws co.unicauca.saberpro.review.domain.exception.ConcurrentReviewUpdateException si otra
     *         operacion lo modifico despues de leerlo (bloqueo optimista con {@link QuestionReview#version()}).
     */
    void save(QuestionReview review);

    /** Revisiones cuya ronda actual aun espera veredicto (HU04). */
    List<QuestionReview> findAwaitingDecision();

    /** Revisiones en las que el revisor fue asignado en alguna ronda (HU05). */
    List<QuestionReview> findByReviewer(String reviewerLogin);
}
