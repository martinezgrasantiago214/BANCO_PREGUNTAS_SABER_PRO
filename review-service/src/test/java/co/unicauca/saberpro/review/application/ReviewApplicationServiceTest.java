package co.unicauca.saberpro.review.application;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.common.web.PendingIssueException;
import co.unicauca.saberpro.review.access.InMemoryReviewRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Casos de uso pendientes del review-service: verifican que respondan con su
 * issue. Al implementarlos, reemplace estas pruebas por las del
 * comportamiento real (PSP-35, PSP-50).
 */
class ReviewApplicationServiceTest {

    private final ReviewApplicationService service =
            new ReviewApplicationService(new InMemoryReviewRepository(), new DomainEventBus());

    @Test
    void losCasosDeUsoPendientesIndicanSuIssue() {
        assertPending("PSP-30", () -> service.registerPendingQuestion(
                new QuestionSubmittedForReviewEvent("q-1", "autor1", "¿P?", Instant.now())));
        assertPending("PSP-30", service::listPendingQuestions);
        assertPending("PSP-32", () -> service.assignReviewers("q-1", List.of("rev1"), "admin"));
        assertPending("PSP-47", () -> service.evaluate("q-1", "rev1", EvaluationVerdict.APROBADA, "Bien"));
        assertPending("PSP-47", () -> service.listEvaluations("q-1"));
    }

    private void assertPending(String issue, Runnable call) {
        PendingIssueException ex = assertThrows(PendingIssueException.class, call::run);
        assertEquals(issue, ex.getIssueKey());
    }
}
