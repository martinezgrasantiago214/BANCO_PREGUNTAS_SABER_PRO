package co.unicauca.saberpro.question.domain.state;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.question.domain.Question;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Patron State: transiciones permitidas y prohibidas del ciclo de vida de una pregunta. */
class QuestionStateTest {

    private Question nueva() {
        return Question.builder().type("MULTIPLE_CHOICE").build();
    }

    @Test
    void hu02UnBorradorPasaAPendienteDeRevision() {
        Question q = nueva();
        q.submitForReview();

        assertEquals(QuestionStatus.PENDIENTE_REVISION, q.getStatus());
        assertFalse(q.isEditable());
    }

    @Test
    void hu05UnaPendientePuedeAprobarseORechazarse() {
        Question aprobada = nueva();
        aprobada.submitForReview();
        aprobada.approve();
        assertEquals(QuestionStatus.APROBADA, aprobada.getStatus());

        Question rechazada = nueva();
        rechazada.submitForReview();
        rechazada.reject();
        assertEquals(QuestionStatus.RECHAZADA, rechazada.getStatus());
    }

    @Test
    void unBorradorNoSePuedeEvaluar() {
        Question q = nueva();

        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class, q::approve);
        assertTrue(ex.getMessage().contains("Borrador"));
        assertThrows(InvalidStateTransitionException.class, q::reject);
    }

    @Test
    void unaPendienteNoSePuedeReenviarNiEliminar() {
        Question q = nueva();
        q.submitForReview();

        assertThrows(InvalidStateTransitionException.class, q::submitForReview);
        assertThrows(InvalidStateTransitionException.class, q::delete);
    }

    @Test
    void soloUnBorradorSePuedeEliminarYLuegoNoAdmiteAcciones() {
        Question q = nueva();
        q.delete();

        assertEquals(QuestionStatus.ELIMINADA, q.getStatus());
        assertThrows(InvalidStateTransitionException.class, q::submitForReview);
        assertThrows(InvalidStateTransitionException.class, q::delete);
    }

    @Test
    void losEstadosFinalesNoAdmitenMasTransiciones() {
        for (QuestionStatus finalStatus : new QuestionStatus[]{QuestionStatus.APROBADA, QuestionStatus.RECHAZADA}) {
            QuestionState state = QuestionStates.from(finalStatus);
            assertFalse(state.isEditable());
            assertThrows(InvalidStateTransitionException.class, state::submitForReview);
            assertThrows(InvalidStateTransitionException.class, state::approve);
            assertThrows(InvalidStateTransitionException.class, state::reject);
            assertThrows(InvalidStateTransitionException.class, state::delete);
        }
    }

    @Test
    void cadaStatusTieneSuObjetoEstado() {
        for (QuestionStatus status : QuestionStatus.values()) {
            assertEquals(status, QuestionStates.from(status).status());
        }
    }
}
