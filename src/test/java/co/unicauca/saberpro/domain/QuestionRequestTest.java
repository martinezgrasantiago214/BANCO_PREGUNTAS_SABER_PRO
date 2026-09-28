package co.unicauca.saberpro.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del DTO QuestionRequest (datos capturados en HU01). */
class QuestionRequestTest {

    @Test
    void transportaTodosLosCamposDeHU01() {
        QuestionRequest r = new QuestionRequest("MULTIPLE_CHOICE", "Contexto", "¿Pregunta?",
                Arrays.asList("A", "B", "C", "D"), "B", "Justificacion", "Bibliografia",
                "Competencia", "Tema", "Subtema", DifficultyLevel.ALTO, "autor1");

        assertEquals("MULTIPLE_CHOICE", r.getType());
        assertEquals("Contexto", r.getContext());
        assertEquals("¿Pregunta?", r.getDirectQuestion());
        assertEquals(4, r.getDistractors().size());
        assertEquals("B", r.getCorrectAnswer());
        assertEquals("Justificacion", r.getJustification());
        assertEquals("Bibliografia", r.getBibliography());
        assertEquals("Competencia", r.getCompetence());
        assertEquals("Tema", r.getTopic());
        assertEquals("Subtema", r.getSubtopic());
        assertEquals(DifficultyLevel.ALTO, r.getDifficultyLevel());
        assertEquals("autor1", r.getAuthorLogin());
    }
}
