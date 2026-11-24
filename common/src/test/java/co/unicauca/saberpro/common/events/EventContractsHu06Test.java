package co.unicauca.saberpro.common.events;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HU06: contratos de eventos con version de la pregunta. Los mensajes del
 * segundo corte (sin version) se siguen leyendo como version 1.
 */
class EventContractsHu06Test {

    private static final Instant T0 = Instant.parse("2026-11-24T10:00:00Z");
    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void submittedForReviewWithoutVersionMeansVersionOne() {
        assertEquals(1, new QuestionSubmittedForReviewEvent("q-1", "autor1", "¿P?", T0).questionVersion());
        assertEquals(1, new QuestionSubmittedForReviewEvent("q-1", 0, "autor1", "¿P?", T0).questionVersion());
        assertEquals(3, new QuestionSubmittedForReviewEvent("q-1", 3, "autor1", "¿P?", T0).questionVersion());
    }

    @Test
    void evaluatedWithoutVersionMeansVersionOne() {
        QuestionEvaluatedEvent event = new QuestionEvaluatedEvent("q-1", "autor1", "¿P?", "rev1",
                EvaluationVerdict.RECHAZADA, "Obs", T0);
        assertEquals(1, event.questionVersion());
    }

    @Test
    void jsonRoundTripKeepsVersion() throws Exception {
        QuestionEvaluatedEvent event = new QuestionEvaluatedEvent("q-1", 2, "autor1", "¿P?", "rev1",
                EvaluationVerdict.APROBADA, "Bien", T0);
        String json = mapper.writeValueAsString(event);
        assertEquals(event, mapper.readValue(json, QuestionEvaluatedEvent.class));
    }

    @Test
    void secondCutMessageWithoutVersionIsReadAsVersionOne() throws Exception {
        String json = "{\"questionId\":\"q-1\",\"authorLogin\":\"autor1\",\"directQuestion\":\"¿P?\","
                + "\"occurredAt\":\"2026-11-24T10:00:00Z\"}";
        QuestionSubmittedForReviewEvent event = mapper.readValue(json, QuestionSubmittedForReviewEvent.class);
        assertEquals(1, event.questionVersion());
        assertEquals("q-1", event.questionId());
    }

    @Test
    void resubmittedCopiesReviewersAndToleratesNull() {
        List<String> reviewers = new ArrayList<>(List.of("rev1", "rev2"));
        QuestionResubmittedForReviewEvent event = new QuestionResubmittedForReviewEvent("q-1", 2, 2, "autor1",
                "¿P?", reviewers, "Mejorar distractores", T0);
        reviewers.add("rev3");
        assertEquals(List.of("rev1", "rev2"), event.previousReviewers());
        assertThrows(UnsupportedOperationException.class, () -> event.previousReviewers().add("x"));
        assertEquals(List.of(), new QuestionResubmittedForReviewEvent("q-1", 2, 2, "autor1", "¿P?", null, null, T0)
                .previousReviewers());
    }

    @Test
    void newEventsUseHu06Topics() {
        assertEquals(EventTopics.QUESTION_REVISED,
                new QuestionRevisedEvent("q-1", 2, "autor1", "¿P?", T0).routingKey());
        assertEquals(EventTopics.QUESTION_RESUBMITTED,
                new QuestionResubmittedForReviewEvent("q-1", 2, 2, "autor1", "¿P?", List.of(), null, T0).routingKey());
    }
}
