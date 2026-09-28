package co.unicauca.saberpro.microkernel;

import co.unicauca.saberpro.domain.DifficultyLevel;
import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionPipeline;
import co.unicauca.saberpro.microkernel.pipeline.filters.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** HU03: pruebas del pipeline de Tuberias y Filtros (validacion estructural). */
class QuestionPipelineTest {

    private QuestionPipeline fullPipeline() {
        return new QuestionPipeline()
                .addFilter(new ContentValidationFilter())
                .addFilter(new OptionsValidationFilter())
                .addFilter(new ClassificationFilter())
                .addFilter(new CorrectAnswerValidationFilter())
                .addFilter(new MetadataValidationFilter());
    }

    private QuestionRequest validRequest() {
        List<String> options = Arrays.asList("Opcion A", "Opcion B", "Opcion C", "Opcion D");
        return new QuestionRequest("MULTIPLE_CHOICE",
                "Contexto suficientemente descriptivo de la pregunta.",
                "¿Cual de las siguientes opciones es la correcta?",
                options, "Opcion B", "Justificacion valida", "Bibliografia valida",
                "Arquitectura de software", "Patrones", "MVC", DifficultyLevel.MEDIO, "autor1");
    }

    @Test
    void pipelineAceptaSolicitudCompletaYValida() {
        assertTrue(fullPipeline().execute(validRequest()));
    }

    @Test
    void contentValidationFilterRechazaContenidoVacio() {
        QuestionRequest request = new QuestionRequest("MULTIPLE_CHOICE", "", "corta",
                Arrays.asList("a", "b", "c", "d"), "a", "j", "b", "c", "t", "s",
                DifficultyLevel.BAJO, "autor1");
        assertFalse(new ContentValidationFilter().process(request));
    }

    @Test
    void optionsValidationFilterExigeCuatroOpcionesUnicas() {
        QuestionRequest request = validRequest();

        QuestionRequest tresOpciones = new QuestionRequest(request.getType(), request.getContext(),
                request.getDirectQuestion(), Arrays.asList("a", "b", "c"), "a", request.getJustification(),
                request.getBibliography(), request.getCompetence(), request.getTopic(), request.getSubtopic(),
                request.getDifficultyLevel(), request.getAuthorLogin());
        assertFalse(new OptionsValidationFilter().process(tresOpciones));

        QuestionRequest duplicadas = new QuestionRequest(request.getType(), request.getContext(),
                request.getDirectQuestion(), Arrays.asList("a", "a", "b", "c"), "a", request.getJustification(),
                request.getBibliography(), request.getCompetence(), request.getTopic(), request.getSubtopic(),
                request.getDifficultyLevel(), request.getAuthorLogin());
        assertFalse(new OptionsValidationFilter().process(duplicadas));

        assertTrue(new OptionsValidationFilter().process(request));
    }

    @Test
    void classificationFilterExigeCompetenciaTemaSubtemaYNivel() {
        QuestionRequest request = validRequest();
        assertTrue(new ClassificationFilter().process(request));

        QuestionRequest sinCompetencia = new QuestionRequest(request.getType(), request.getContext(),
                request.getDirectQuestion(), request.getDistractors(), request.getCorrectAnswer(),
                request.getJustification(), request.getBibliography(), "", request.getTopic(),
                request.getSubtopic(), request.getDifficultyLevel(), request.getAuthorLogin());
        assertFalse(new ClassificationFilter().process(sinCompetencia));
    }

    @Test
    void correctAnswerValidationFilterExigeQueLaRespuestaEsteEnLasOpciones() {
        QuestionRequest request = validRequest();
        assertTrue(new CorrectAnswerValidationFilter().process(request));

        QuestionRequest respuestaInvalida = new QuestionRequest(request.getType(), request.getContext(),
                request.getDirectQuestion(), request.getDistractors(), "Opcion inexistente",
                request.getJustification(), request.getBibliography(), request.getCompetence(),
                request.getTopic(), request.getSubtopic(), request.getDifficultyLevel(), request.getAuthorLogin());
        assertFalse(new CorrectAnswerValidationFilter().process(respuestaInvalida));
    }

    @Test
    void metadataValidationFilterExigeJustificacionBibliografiaYAutor() {
        QuestionRequest request = validRequest();
        assertTrue(new MetadataValidationFilter().process(request));

        QuestionRequest sinJustificacion = new QuestionRequest(request.getType(), request.getContext(),
                request.getDirectQuestion(), request.getDistractors(), request.getCorrectAnswer(),
                "", request.getBibliography(), request.getCompetence(), request.getTopic(),
                request.getSubtopic(), request.getDifficultyLevel(), request.getAuthorLogin());
        assertFalse(new MetadataValidationFilter().process(sinJustificacion));
    }

    @Test
    void pipelineSeDetieneEnElPrimerFiltroQueFalla() {
        QuestionPipeline pipeline = fullPipeline();
        QuestionRequest invalido = new QuestionRequest("MULTIPLE_CHOICE", "", "x",
                Collections.emptyList(), "", "", "", "", "", "", null, "");
        assertFalse(pipeline.execute(invalido));
        assertNotNull(pipeline.getLastError());
    }
}
