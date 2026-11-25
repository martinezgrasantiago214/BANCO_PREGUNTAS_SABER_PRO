package co.unicauca.saberpro.review.infrastructure.adapter.out.events;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.events.DomainEvent;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.EventTopics;
import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.events.QuestionResubmittedForReviewEvent;
import co.unicauca.saberpro.common.events.ReviewersAssignedEvent;
import co.unicauca.saberpro.review.domain.event.QuestionEvaluated;
import co.unicauca.saberpro.review.domain.event.ReviewRoundReopened;
import co.unicauca.saberpro.review.domain.event.ReviewersAssigned;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Adaptador de eventos: traduce los eventos del dominio al contrato de integracion entre servicios. */
class DomainEventBusReviewPublisherTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    @Test
    void publicaCadaEventoDelDominioComoEventoDeIntegracion() {
        DomainEventBus bus = new DomainEventBus();
        List<DomainEvent> sent = new ArrayList<>();
        bus.subscribe(sent::add);

        new DomainEventBusReviewPublisher(bus).publish(List.of(
                new ReviewersAssigned("q-1", 1, "¿P?", List.of("rev1", "rev2"), "admin", NOW),
                new QuestionEvaluated("q-1", 1, 1, "autor1", "¿P?", "rev1", EvaluationVerdict.RECHAZADA, "Ajustar", NOW),
                new ReviewRoundReopened("q-1", 2, 2, "autor1", "¿P v2?", List.of("rev1"), "Ajustar", NOW)));

        assertEquals(3, sent.size());
        ReviewersAssignedEvent assigned = (ReviewersAssignedEvent) sent.get(0);
        assertEquals(List.of("rev1", "rev2"), assigned.reviewerLogins());
        assertEquals(EventTopics.REVIEWERS_ASSIGNED, assigned.routingKey());

        QuestionEvaluatedEvent evaluated = (QuestionEvaluatedEvent) sent.get(1);
        assertEquals(EvaluationVerdict.RECHAZADA, evaluated.verdict());
        assertEquals(1, evaluated.questionVersion());
        assertEquals("autor1", evaluated.authorLogin());
        assertEquals(EventTopics.QUESTION_EVALUATED, evaluated.routingKey());

        QuestionResubmittedForReviewEvent resubmitted = (QuestionResubmittedForReviewEvent) sent.get(2);
        assertEquals(2, resubmitted.questionVersion());
        assertEquals(2, resubmitted.round());
        assertEquals(List.of("rev1"), resubmitted.previousReviewers());
        assertEquals(EventTopics.QUESTION_RESUBMITTED, resubmitted.routingKey());
    }
}
