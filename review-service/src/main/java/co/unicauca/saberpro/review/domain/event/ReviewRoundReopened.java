package co.unicauca.saberpro.review.domain.event;

import java.time.Instant;
import java.util.List;

/**
 * HU06: llego una version mejorada de una pregunta rechazada y se abrio una
 * ronda nueva. Lleva lo que necesitan las notificaciones: quienes revisaron
 * la ronda anterior y que observaciones dejaron.
 */
public record ReviewRoundReopened(String questionId, int questionVersion, int round, String authorLogin,
                                  String directQuestion, List<String> previousReviewers,
                                  String previousObservations, Instant occurredAt) implements ReviewDomainEvent {

    public ReviewRoundReopened {
        previousReviewers = List.copyOf(previousReviewers);
    }
}
