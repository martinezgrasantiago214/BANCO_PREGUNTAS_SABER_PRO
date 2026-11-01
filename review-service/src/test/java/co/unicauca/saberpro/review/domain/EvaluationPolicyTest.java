package co.unicauca.saberpro.review.domain;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Reglas de HU05 sobre el contenido de una evaluacion (PSP-47). */
class EvaluationPolicyTest {

    @Test
    void unaAprobacionNoNecesitaObservaciones() {
        assertTrue(EvaluationPolicy.validate(EvaluationVerdict.APROBADA, null).isEmpty());
        assertTrue(EvaluationPolicy.validate(EvaluationVerdict.APROBADA, "").isEmpty());
    }

    @Test
    void unRechazoExigeObservaciones() {
        List<String> errors = EvaluationPolicy.validate(EvaluationVerdict.RECHAZADA, "   ");

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("debe dejar observaciones"));
        assertTrue(EvaluationPolicy.validate(EvaluationVerdict.RECHAZADA, "Corregir el distractor C").isEmpty());
    }

    @Test
    void elVeredictoEsObligatorio() {
        assertFalse(EvaluationPolicy.validate(null, "Bien").isEmpty());
    }

    @Test
    void lasObservacionesTienenUnTope() {
        String max = "x".repeat(EvaluationPolicy.MAX_OBSERVATIONS_LENGTH);

        assertTrue(EvaluationPolicy.validate(EvaluationVerdict.APROBADA, max).isEmpty());
        assertFalse(EvaluationPolicy.validate(EvaluationVerdict.APROBADA, max + "x").isEmpty());
    }
}
