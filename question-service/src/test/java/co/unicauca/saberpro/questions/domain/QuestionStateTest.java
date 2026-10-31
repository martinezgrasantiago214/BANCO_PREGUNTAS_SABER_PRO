package co.unicauca.saberpro.questions.domain;

import co.unicauca.saberpro.questions.domain.exception.QuestionDomainException;
import co.unicauca.saberpro.questions.domain.lifecycle.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del patron State: transiciones validas e invalidas. */
class QuestionStateTest {

    @Test
    @DisplayName("Borrador -> Pendiente -> En revision -> Aprobada")
    void happyPath() {
        QuestionState s = QuestionStateType.BORRADOR.toState();
        s = s.submitForReview();
        assertEquals(QuestionStateType.PENDIENTE_REVISION, s.type());
        s = s.startReview();
        assertEquals(QuestionStateType.EN_REVISION, s.type());
        assertEquals(QuestionStateType.APROBADA, s.approve().type());
        assertEquals(QuestionStateType.RECHAZADA, s.reject().type());
    }

    @Test
    void rejectedGoesBackToDraftWhenEdited() {
        QuestionState rejected = QuestionStateType.RECHAZADA.toState();
        assertTrue(rejected.isEditable());
        assertTrue(rejected.isDeletable());
        assertEquals(QuestionStateType.BORRADOR, rejected.edit().type());
    }

    @Test
    void draftIsEditableAndKeepsStateWhenEdited() {
        QuestionState draft = DraftState.INSTANCE;
        assertTrue(draft.isEditable());
        assertSame(draft, draft.edit());
    }

    @Test
    void invalidTransitionsThrow() {
        assertTransitionFails(() -> DraftState.INSTANCE.approve());
        assertTransitionFails(() -> DraftState.INSTANCE.startReview());
        assertTransitionFails(() -> PendingReviewState.INSTANCE.edit());
        assertTransitionFails(() -> PendingReviewState.INSTANCE.submitForReview());
        assertTransitionFails(() -> InReviewState.INSTANCE.edit());
        assertTransitionFails(() -> ApprovedState.INSTANCE.reject());
        assertTransitionFails(() -> ApprovedState.INSTANCE.edit());
        assertTransitionFails(() -> RejectedState.INSTANCE.submitForReview());
    }

    @Test
    void onlyDraftAndRejectedAreDeletable() {
        for (QuestionStateType t : QuestionStateType.values()) {
            boolean expected = t == QuestionStateType.BORRADOR || t == QuestionStateType.RECHAZADA;
            assertEquals(expected, t.toState().isDeletable(), t.name());
        }
    }

    @Test
    void everyStateHasLabelAndColor() {
        for (QuestionStateType t : QuestionStateType.values()) {
            assertFalse(t.getLabel().isBlank());
            assertTrue(t.getColorHex().matches("#[0-9A-F]{6}"), t.name());
            assertEquals(t, t.toState().type());
        }
    }

    private static void assertTransitionFails(org.junit.jupiter.api.function.Executable action) {
        QuestionDomainException ex = assertThrows(QuestionDomainException.class, action);
        assertEquals(QuestionDomainException.Kind.INVALID_TRANSITION, ex.getKind());
    }
}
