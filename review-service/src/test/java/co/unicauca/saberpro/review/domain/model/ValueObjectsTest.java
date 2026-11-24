package co.unicauca.saberpro.review.domain.model;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.review.domain.exception.ReviewRuleViolationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Objetos de valor y estados del contexto de revision. */
class ValueObjectsTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    @Test
    void laEvaluacionValidaSuContenidoYRecortaLasObservaciones() {
        Evaluation ok = Evaluation.issue("rev1", EvaluationVerdict.RECHAZADA, "  Corregir C  ", NOW);
        assertEquals("Corregir C", ok.observations());
        assertTrue(ok.isRejection());
        assertNotNull(ok.id());

        assertTrue(Evaluation.validate(EvaluationVerdict.APROBADA, null).isEmpty());
        assertEquals(1, Evaluation.validate(null, "x").size());
        assertEquals(1, Evaluation.validate(EvaluationVerdict.RECHAZADA, "").size());
        String larga = "x".repeat(Evaluation.MAX_OBSERVATIONS_LENGTH + 1);
        assertEquals(1, Evaluation.validate(EvaluationVerdict.APROBADA, larga).size());
        assertThrows(ReviewRuleViolationException.class,
                () -> Evaluation.issue("rev1", EvaluationVerdict.RECHAZADA, " ", NOW));
    }

    @Test
    void laSeleccionDeRevisoresRecortaLosLoginsYAplicaLaRegla() {
        assertEquals(List.of("rev1", "rev2"), new ReviewerSelection(List.of(" rev1", "rev2 ")).logins());
        assertThrows(ReviewRuleViolationException.class, () -> new ReviewerSelection(null));
        assertThrows(ReviewRuleViolationException.class, () -> new ReviewerSelection(Arrays.asList("rev1", null)));
        assertThrows(ReviewRuleViolationException.class, () -> new ReviewerSelection(List.of("a", "b", "c", "d")));
        assertThrows(ReviewRuleViolationException.class, () -> new ReviewerSelection(List.of("a", " a")));
    }

    @Test
    void elCandidatoEsRevisorSoloSiExisteYTieneElRol() {
        assertTrue(ReviewerCandidate.of("rev1", Role.REVISOR).isReviewer());
        assertFalse(ReviewerCandidate.of("autor1", Role.AUTOR_PREGUNTAS).isReviewer());
        assertFalse(ReviewerCandidate.unknown("x").isReviewer());
    }

    @Test
    void elEnvioNecesitaLaPreguntaYNormalizaLaVersion() {
        QuestionSubmission s = new QuestionSubmission(" q-9 ", 0, " autor1 ", "¿?", null);
        assertEquals("q-9", s.questionId());
        assertEquals(1, s.questionVersion());
        assertEquals("autor1", s.authorLogin());
        assertNotNull(s.submittedAt());
        assertThrows(ReviewRuleViolationException.class, () -> new QuestionSubmission(" ", 1, "a", "b", NOW));
    }

    @Test
    void cadaEstadoDiceQueOperacionesAdmite() {
        assertTrue(ReviewStatus.PENDIENTE_ASIGNACION.acceptsAssignments());
        assertFalse(ReviewStatus.PENDIENTE_ASIGNACION.acceptsEvaluation());
        assertTrue(ReviewStatus.EN_REVISION.acceptsAssignments());
        assertTrue(ReviewStatus.EN_REVISION.acceptsEvaluation());
        assertTrue(ReviewStatus.APROBADA.isDecided());
        assertFalse(ReviewStatus.APROBADA.allowsNewVersion());
        assertTrue(ReviewStatus.RECHAZADA.allowsNewVersion());
        assertEquals(ReviewStatus.APROBADA, ReviewStatus.decidedBy(EvaluationVerdict.APROBADA));
        assertEquals(ReviewStatus.RECHAZADA, ReviewStatus.decidedBy(EvaluationVerdict.RECHAZADA));
    }

    @Test
    void unaRondaGuardadaDebeSerCoherenteConSuEstado() {
        QuestionSubmission s = new QuestionSubmission("q", 1, "autor1", "¿?", NOW);
        assertThrows(IllegalStateException.class,
                () -> ReviewRound.restore(1, s, ReviewStatus.RECHAZADA, List.of(), null));
        assertThrows(IllegalStateException.class, () -> ReviewRound.restore(1, s, ReviewStatus.PENDIENTE_ASIGNACION,
                List.of(new ReviewerAssignment("rev1", "admin", NOW)), null));
        assertThrows(IllegalArgumentException.class,
                () -> ReviewRound.restore(0, s, ReviewStatus.PENDIENTE_ASIGNACION, List.of(), null));
    }
}
