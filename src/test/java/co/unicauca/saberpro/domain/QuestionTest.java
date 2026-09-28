package co.unicauca.saberpro.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {

    @Test
    void nacePorDefectoEnEstadoBorrador() {
        Question q = new Question("id-1", "MULTIPLE_CHOICE", "contexto", "pregunta",
                Arrays.asList("a", "b", "c", "d"), "a", "just", "bib", "comp", "tema", "subtema",
                DifficultyLevel.BAJO, "autor1");
        assertEquals(QuestionState.BORRADOR, q.getState());
    }

    @Test
    void cadaEstadoTieneUnColorAsignadoParaHU02() {
        for (QuestionState state : QuestionState.values()) {
            assertNotNull(state.getColor(), "El estado " + state + " debe tener un color asociado.");
        }
    }

    @Test
    void assignReviewerNoDuplicaRevisores() {
        Question q = new Question("id-1", "MULTIPLE_CHOICE", "contexto", "pregunta",
                Arrays.asList("a", "b", "c", "d"), "a", "just", "bib", "comp", "tema", "subtema",
                DifficultyLevel.BAJO, "autor1");

        q.assignReviewer("rev1");
        q.assignReviewer("rev1");
        q.assignReviewer("rev2");

        assertEquals(2, q.getAssignedReviewers().size());
    }

    @Test
    void changeStateActualizaElEstado() {
        Question q = new Question("id-1", "MULTIPLE_CHOICE", "contexto", "pregunta",
                Arrays.asList("a", "b", "c", "d"), "a", "just", "bib", "comp", "tema", "subtema",
                DifficultyLevel.BAJO, "autor1");

        q.changeState(QuestionState.PENDIENTE_REVISION);

        assertEquals(QuestionState.PENDIENTE_REVISION, q.getState());
    }
}
