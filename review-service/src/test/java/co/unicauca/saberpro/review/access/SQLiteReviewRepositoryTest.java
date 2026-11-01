package co.unicauca.saberpro.review.access;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.review.domain.Evaluation;
import co.unicauca.saberpro.review.domain.PendingQuestion;
import co.unicauca.saberpro.review.domain.ReviewAssignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** HU04 (PSP-33 y PSP-35): persistencia SQLite del review-service (con una base temporal). */
class SQLiteReviewRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-30T15:00:00Z");

    @TempDir
    Path tempDir;

    private String url;
    private SQLiteReviewRepository repository;

    @BeforeEach
    void setUp() {
        url = "jdbc:sqlite:" + tempDir.resolve("review-test.db");
        repository = new SQLiteReviewRepository(url);
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    /** Simula reiniciar el servicio: cierra la conexion y abre otra sobre el mismo archivo. */
    private SQLiteReviewRepository restart() {
        repository.close();
        repository = new SQLiteReviewRepository(url);
        return repository;
    }

    @Test
    void lasAsignacionesSobrevivenAUnReinicioDelServicio() {
        repository.savePendingQuestion(new PendingQuestion("q-1", "autor1", "¿P1?", T0));
        repository.saveAssignment(new ReviewAssignment("q-1", "rev1", "admin", T0));
        repository.saveAssignment(new ReviewAssignment("q-1", "rev2", "admin", T0.plusSeconds(1)));
        repository.saveAssignment(new ReviewAssignment("q-2", "rev1", "admin", T0.plusSeconds(2)));

        SQLiteReviewRepository reopened = restart();

        PendingQuestion pending = reopened.findPendingQuestion("q-1").orElseThrow();
        assertEquals("autor1", pending.authorLogin());
        assertEquals("¿P1?", pending.directQuestion());
        assertEquals(T0, pending.submittedAt());

        List<ReviewAssignment> byQuestion = reopened.findAssignmentsByQuestion("q-1");
        assertEquals(List.of("rev1", "rev2"), byQuestion.stream().map(ReviewAssignment::getReviewerLogin).sorted().toList());
        ReviewAssignment first = byQuestion.stream().filter(a -> a.getReviewerLogin().equals("rev1")).findFirst()
                .orElseThrow();
        assertEquals("admin", first.getAssignedBy());
        assertEquals(T0, first.getAssignedAt());

        assertEquals(List.of("q-1", "q-2"), reopened.findAssignmentsByReviewer("rev1").stream()
                .map(ReviewAssignment::getQuestionId).sorted().toList());
        assertTrue(reopened.findAssignmentsByReviewer("rev9").isEmpty());
    }

    @Test
    void noSePuedeGuardarDosVecesElMismoRevisorParaLaMismaPregunta() {
        repository.saveAssignment(new ReviewAssignment("q-1", "rev1", "admin", T0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> repository.saveAssignment(new ReviewAssignment("q-1", "rev1", "admin2", T0.plusSeconds(5))));

        assertTrue(ex.getMessage().contains("rev1 ya esta asignado"));
        assertEquals(1, repository.findAssignmentsByQuestion("q-1").size());
    }

    @Test
    void laPreguntaPendienteSeReemplazaSiLlegaDeNuevo() {
        repository.savePendingQuestion(new PendingQuestion("q-1", "autor1", "¿Version 1?", T0));
        repository.savePendingQuestion(new PendingQuestion("q-1", "autor1", "¿Version 2?", T0.plusSeconds(60)));

        assertEquals(1, repository.findPendingQuestions().size());
        assertEquals("¿Version 2?", repository.findPendingQuestion("q-1").orElseThrow().directQuestion());
        assertTrue(repository.findPendingQuestion("q-9").isEmpty());
    }

    @Test
    void lasEvaluacionesSeGuardanYSobrevivenAUnReinicio() {
        repository.saveEvaluation(new Evaluation("e-1", "q-1", "rev1", EvaluationVerdict.RECHAZADA, "Ajustar C", T0));

        List<Evaluation> evaluations = restart().findEvaluationsByQuestion("q-1");

        assertEquals(1, evaluations.size());
        Evaluation e = evaluations.get(0);
        assertEquals("e-1", e.getId());
        assertEquals("rev1", e.getReviewerLogin());
        assertEquals(EvaluationVerdict.RECHAZADA, e.getVerdict());
        assertEquals("Ajustar C", e.getObservations());
        assertEquals(T0, e.getEvaluatedAt());
        assertTrue(repository.findEvaluationsByQuestion("q-2").isEmpty());
    }
}
