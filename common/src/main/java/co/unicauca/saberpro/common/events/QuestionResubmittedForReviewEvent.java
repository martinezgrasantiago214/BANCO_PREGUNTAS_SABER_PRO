package co.unicauca.saberpro.common.events;

import java.time.Instant;
import java.util.List;

/**
 * HU06: llego a revision una version mejorada de una pregunta que habia sido
 * rechazada, y el review-service abrio una ronda de revision nueva. Lo
 * publica el review-service; el notification-service avisa a los
 * implicados: los administradores (deben asignar revisores otra vez), los
 * revisores de la ronda anterior y el autor.
 */
public record QuestionResubmittedForReviewEvent(String questionId, int questionVersion, int round,
                                                String authorLogin, String directQuestion,
                                                List<String> previousReviewers, String previousObservations,
                                                Instant occurredAt) implements DomainEvent {

    public QuestionResubmittedForReviewEvent {
        previousReviewers = previousReviewers == null ? List.of() : List.copyOf(previousReviewers);
    }

    @Override
    public String routingKey() {
        return EventTopics.QUESTION_RESUBMITTED;
    }
}
