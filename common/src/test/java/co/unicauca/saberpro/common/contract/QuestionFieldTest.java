package co.unicauca.saberpro.common.contract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de las reglas de captura de los campos de texto de una pregunta (HU01, PSP-15). */
class QuestionFieldTest {

    @Test
    void hu01ExistenLosSieteCamposDeTexto() {
        assertEquals(7, QuestionField.values().length);
        assertEquals("Contexto", QuestionField.CONTEXT.getLabel());
        assertEquals("Pregunta directa", QuestionField.DIRECT_QUESTION.toString());
        assertEquals("Justificacion de la respuesta", QuestionField.JUSTIFICATION.getLabel());
        assertEquals("Bibliografia", QuestionField.BIBLIOGRAPHY.getLabel());
        assertEquals("Competencia", QuestionField.COMPETENCE.getLabel());
        assertEquals("Tema", QuestionField.TOPIC.getLabel());
        assertEquals("Subtema", QuestionField.SUBTOPIC.getLabel());
    }

    @Test
    void todosLosCamposTienenLimitesCoherentes() {
        for (QuestionField field : QuestionField.values()) {
            assertTrue(field.getMinLength() >= 1, "Minimo invalido en " + field);
            assertTrue(field.getMaxLength() > field.getMinLength(), "Maximo invalido en " + field);
        }
    }

    @Test
    void campoVacioONuloEsObligatorio() {
        assertEquals("El campo Tema es obligatorio.", QuestionField.TOPIC.validate(null));
        assertEquals("El campo Tema es obligatorio.", QuestionField.TOPIC.validate("   "));
    }

    @Test
    void preguntaDirectaExigeLongitudMinima() {
        assertEquals("El campo Pregunta directa debe tener al menos 10 caracteres.",
                QuestionField.DIRECT_QUESTION.validate("¿Corta?"));
        assertNull(QuestionField.DIRECT_QUESTION.validate("¿Que patron usa plugins?"));
    }

    @Test
    void campoQueSuperaElMaximoEsInvalido() {
        int max = QuestionField.SUBTOPIC.getMaxLength();
        assertNull(QuestionField.SUBTOPIC.validate("x".repeat(max)));
        assertEquals("El campo Subtema no puede superar " + max + " caracteres.",
                QuestionField.SUBTOPIC.validate("x".repeat(max + 1)));
    }

    @Test
    void losEspaciosAlRededorNoCuentan() {
        assertEquals("El campo Pregunta directa debe tener al menos 10 caracteres.",
                QuestionField.DIRECT_QUESTION.validate("   corta   "));
        assertNull(QuestionField.CONTEXT.validate("  Contexto  "));
    }
}
