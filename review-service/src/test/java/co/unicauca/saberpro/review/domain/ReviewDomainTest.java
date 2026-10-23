package co.unicauca.saberpro.review.domain;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Entidades y reglas del contexto de revision por pares. */
class ReviewDomainTest {

    @Test
    void hu04SeAceptanEntreUnoYTresRevisoresDistintos() {
        assertTrue(ReviewPolicy.validateReviewers(List.of("rev1")).isEmpty());
        assertTrue(ReviewPolicy.validateReviewers(List.of("rev1", "rev2", "rev3")).isEmpty());
    }

    @Test
    void hu04SeRechazaNingunRevisorOMasDeTres() {
        assertFalse(ReviewPolicy.validateReviewers(List.of()).isEmpty());
        assertFalse(ReviewPolicy.validateReviewers(null).isEmpty());
        assertFalse(ReviewPolicy.validateReviewers(List.of("r1", "r2", "r3", "r4")).isEmpty());
    }

    @Test
    void hu04SeRechazanRevisoresRepetidosOVacios() {
        assertFalse(ReviewPolicy.validateReviewers(List.of("rev1", "rev1")).isEmpty());
        assertFalse(ReviewPolicy.validateReviewers(Arrays.asList("rev1", " ")).isEmpty());
    }

    @Test
    void unaAsignacionGuardaQuienAsignoYCuando() {
        Instant now = Instant.now();
        ReviewAssignment a = new ReviewAssignment("q-1", "rev1", "admin", now);

        assertEquals("q-1", a.getQuestionId());
        assertEquals("rev1", a.getReviewerLogin());
        assertEquals("admin", a.getAssignedBy());
        assertEquals(now, a.getAssignedAt());
        assertTrue(a.isFor("q-1", "rev1"));
        assertFalse(a.isFor("q-1", "rev2"));
        assertNotNull(new ReviewAssignment("q-1", "rev1", "admin", null).getAssignedAt());
    }

    @Test
    void unaAsignacionExigePreguntaYRevisor() {
        assertThrows(NullPointerException.class, () -> new ReviewAssignment(null, "rev1", "admin", null));
        assertThrows(NullPointerException.class, () -> new ReviewAssignment("q-1", null, "admin", null));
    }

    @Test
    void hu05UnaEvaluacionGuardaVeredictoYObservaciones() {
        Evaluation e = new Evaluation("q-1", "rev1", EvaluationVerdict.RECHAZADA, "  Revisar el distractor C ");

        assertNotNull(e.getId());
        assertEquals("q-1", e.getQuestionId());
        assertEquals("rev1", e.getReviewerLogin());
        assertEquals(EvaluationVerdict.RECHAZADA, e.getVerdict());
        assertEquals("Revisar el distractor C", e.getObservations());
        assertNotNull(e.getEvaluatedAt());
    }

    @Test
    void hu05UnaEvaluacionSinObservacionesQuedaConTextoVacioYExigeVeredicto() {
        assertEquals("", new Evaluation("q-1", "rev1", EvaluationVerdict.APROBADA, null).getObservations());
        assertThrows(NullPointerException.class, () -> new Evaluation("q-1", "rev1", null, "x"));
    }

    @Test
    void unaPreguntaPendienteConservaLosDatosDelEvento() {
        Instant now = Instant.now();
        PendingQuestion p = new PendingQuestion("q-1", "autor1", "¿Pregunta?", now);
        assertEquals("q-1", p.questionId());
        assertEquals("autor1", p.authorLogin());
        assertEquals(now, p.submittedAt());
    }
}
