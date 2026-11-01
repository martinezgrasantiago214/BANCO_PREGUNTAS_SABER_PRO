package co.unicauca.saberpro.review.access;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.review.domain.Evaluation;
import co.unicauca.saberpro.review.domain.PendingQuestion;
import co.unicauca.saberpro.review.domain.ReviewAssignment;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryReviewRepositoryTest {

    private final InMemoryReviewRepository repository = new InMemoryReviewRepository();

    @Test
    void guardaYConsultaPreguntasPendientes() {
        repository.savePendingQuestion(new PendingQuestion("q-1", "autor1", "¿P1?", Instant.now()));
        repository.savePendingQuestion(new PendingQuestion("q-2", "autor1", "¿P2?", Instant.now()));

        assertEquals(2, repository.findPendingQuestions().size());
        assertTrue(repository.findPendingQuestion("q-1").isPresent());
        assertTrue(repository.findPendingQuestion("q-9").isEmpty());
    }

    @Test
    void consultaAsignacionesPorPreguntaYPorRevisor() {
        repository.saveAssignment(new ReviewAssignment("q-1", "rev1", "admin", null));
        repository.saveAssignment(new ReviewAssignment("q-1", "rev2", "admin", null));
        repository.saveAssignment(new ReviewAssignment("q-2", "rev1", "admin", null));

        assertEquals(2, repository.findAssignmentsByQuestion("q-1").size());
        assertEquals(2, repository.findAssignmentsByReviewer("rev1").size());
        assertTrue(repository.findAssignmentsByReviewer("rev3").isEmpty());
    }

    @Test
    void consultaEvaluacionesPorPregunta() {
        repository.saveEvaluation(new Evaluation("q-1", "rev1", EvaluationVerdict.APROBADA, "Bien"));

        assertEquals(1, repository.findEvaluationsByQuestion("q-1").size());
        assertTrue(repository.findEvaluationsByQuestion("q-2").isEmpty());
    }
}
