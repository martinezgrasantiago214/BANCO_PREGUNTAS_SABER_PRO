package co.unicauca.saberpro.microkernel;

import co.unicauca.saberpro.domain.DifficultyLevel;
import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.microkernel.core.QuestionMicrokernel;
import co.unicauca.saberpro.microkernel.plugins.MultipleChoiceQuestionPlugin;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del nucleo (Microkernel): registro dinamico de plugins via
 * Reflexion, y ejecucion del plugin correcto segun el tipo solicitado.
 */
class QuestionMicrokernelTest {

    @Test
    void cargaLosTresPluginsDeclaradosEnPluginsProperties() {
        QuestionMicrokernel microkernel = new QuestionMicrokernel();
        assertEquals(3, microkernel.getPlugins().size());
    }

    @Test
    void executePluginGeneraYAlmacenaUnaPreguntaValida() {
        QuestionMicrokernel microkernel = new QuestionMicrokernel();
        QuestionRequest request = new QuestionRequest(
                MultipleChoiceQuestionPlugin.TYPE,
                "Contexto valido y suficientemente largo.",
                "¿Cual es la respuesta correcta de esta pregunta de ejemplo?",
                Arrays.asList("Opcion A", "Opcion B", "Opcion C", "Opcion D"),
                "Opcion A", "Justificacion", "Bibliografia",
                "Arquitectura de software", "Tema", "Subtema", DifficultyLevel.ALTO, "autor1");

        Question result = microkernel.executePlugin(MultipleChoiceQuestionPlugin.TYPE, request);

        assertNotNull(result);
        assertEquals(1, microkernel.getQuestions().size());
        assertTrue(microkernel.getQuestions().containsKey(result.getId()));
    }

    @Test
    void executePluginDevuelveNullCuandoLaValidacionFalla() {
        QuestionMicrokernel microkernel = new QuestionMicrokernel();
        QuestionRequest invalido = new QuestionRequest(MultipleChoiceQuestionPlugin.TYPE,
                "", "x", Arrays.asList("a"), "", "", "", "", "", "", null, "");

        Question result = microkernel.executePlugin(MultipleChoiceQuestionPlugin.TYPE, invalido);

        assertNull(result);
        assertNotNull(microkernel.getLastError());
        assertTrue(microkernel.getQuestions().isEmpty());
    }

    @Test
    void executePluginLanzaExcepcionSiNingunPluginSoportaElTipo() {
        QuestionMicrokernel microkernel = new QuestionMicrokernel();
        QuestionRequest request = new QuestionRequest("TIPO_INEXISTENTE",
                "ctx", "pregunta", Arrays.asList("a", "b", "c", "d"), "a", "j", "b",
                "c", "t", "s", DifficultyLevel.BAJO, "autor1");

        assertThrows(IllegalArgumentException.class,
                () -> microkernel.executePlugin("TIPO_INEXISTENTE", request));
    }
}
