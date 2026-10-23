package co.unicauca.saberpro.question.domain;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.question.TestData;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Patron Builder: construccion de la entidad Question. */
class QuestionBuilderTest {

    @Test
    void construyeUnaPreguntaConTodosLosCamposDeHU01() {
        Question q = Question.builder().type("MULTIPLE_CHOICE").fromRequest(TestData.validRequest("x")).build();

        assertEquals("MULTIPLE_CHOICE", q.getType());
        assertEquals("Una empresa necesita que su banco de preguntas admita nuevos tipos.", q.getContext());
        assertEquals("¿Que patron permite agregar funcionalidad mediante plugins?", q.getDirectQuestion());
        assertEquals(4, q.getDistractors().size());
        assertEquals("Microkernel", q.getCorrectAnswer());
        assertTrue(q.getJustification().startsWith("El patron"));
        assertTrue(q.getBibliography().startsWith("Richards"));
        assertEquals("Arquitectura de software", q.getCompetence());
        assertEquals("Patrones arquitectonicos", q.getTopic());
        assertEquals("Microkernel", q.getSubtopic());
        assertEquals(DifficultyLevel.MEDIO, q.getDifficultyLevel());
        assertEquals("autor1", q.getAuthorLogin());
    }

    @Test
    void porDefectoGeneraIdFechaYNaceEnBorrador() {
        Question q = Question.builder().type("MULTIPLE_CHOICE").build();

        assertNotNull(q.getId());
        assertNotNull(q.getCreatedAt());
        assertEquals(QuestionStatus.BORRADOR, q.getStatus());
        assertTrue(q.isEditable());
        assertTrue(q.getDistractors().isEmpty());
    }

    @Test
    void reconstruyeUnaPreguntaGuardadaConSuIdFechaYEstado() {
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 1, 8, 30);
        Question q = Question.builder().id("q-1").type("CASO").createdAt(fecha)
                .status(QuestionStatus.APROBADA).build();

        assertEquals("q-1", q.getId());
        assertEquals(fecha, q.getCreatedAt());
        assertEquals(QuestionStatus.APROBADA, q.getStatus());
        assertFalse(q.isEditable());
    }

    @Test
    void sinTipoNoSePuedeConstruir() {
        assertThrows(IllegalStateException.class, () -> Question.builder().build());
    }

    @Test
    void losDistractoresNoSePuedenModificarDesdeAfuera() {
        List<String> opciones = new ArrayList<>(List.of("a", "b", "c", "d"));
        Question q = Question.builder().type("MULTIPLE_CHOICE").distractors(opciones).build();
        opciones.add("e");

        assertEquals(4, q.getDistractors().size());
        assertThrows(UnsupportedOperationException.class, () -> q.getDistractors().add("f"));
    }

    @Test
    void identificaAlAutorYSeMuestraConSuEstado() {
        Question q = Question.builder().type("MULTIPLE_CHOICE").authorLogin("autor1").directQuestion("¿P?").build();

        assertTrue(q.isAuthoredBy("autor1"));
        assertFalse(q.isAuthoredBy("autor2"));
        assertTrue(q.toString().contains("Borrador"));
    }
}
