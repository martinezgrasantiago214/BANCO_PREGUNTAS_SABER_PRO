package co.unicauca.saberpro.question.domain.validation;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.QuestionField;
import co.unicauca.saberpro.question.TestData;
import co.unicauca.saberpro.question.domain.QuestionRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/** Validacion estructural: cada filtro y el pipeline completo. */
class QuestionPipelineTest {

    private QuestionPipeline fullPipeline() {
        return new QuestionPipeline()
                .addFilter(new ContentValidationFilter())
                .addFilter(new OptionsValidationFilter())
                .addFilter(new ClassificationFilter())
                .addFilter(new CorrectAnswerValidationFilter())
                .addFilter(new MetadataValidationFilter());
    }

    private QuestionRequest valid() {
        return TestData.validRequest("MULTIPLE_CHOICE");
    }

    private QuestionRequest copy(QuestionRequest r, String context, String directQuestion, java.util.List<String> options,
                                 String competence, String justification) {
        return new QuestionRequest(r.getType(), context, directQuestion, options, r.getCorrectAnswer(), justification,
                r.getBibliography(), competence, r.getTopic(), r.getSubtopic(), r.getDifficultyLevel(),
                r.getAuthorLogin());
    }

    @Test
    void elPipelineAceptaUnaSolicitudCompletaYValida() {
        QuestionPipeline pipeline = fullPipeline();
        assertTrue(pipeline.execute(valid()));
        assertNull(pipeline.getLastError());
    }

    @Test
    void contentValidationFilterRechazaContenidoVacioOPreguntaCorta() {
        QuestionRequest r = valid();
        assertFalse(new ContentValidationFilter().process(copy(r, "", r.getDirectQuestion(), r.getDistractors(),
                r.getCompetence(), r.getJustification())));
        assertFalse(new ContentValidationFilter().process(copy(r, r.getContext(), "corta", r.getDistractors(),
                r.getCompetence(), r.getJustification())));
    }

    @Test
    void elMinimoDeLaPreguntaDirectaEsElMismoQueUsaElFormulario() {
        int min = QuestionField.DIRECT_QUESTION.getMinLength();
        QuestionRequest r = valid();
        ContentValidationFilter filter = new ContentValidationFilter();

        assertFalse(filter.process(copy(r, r.getContext(), "x".repeat(min - 1), r.getDistractors(),
                r.getCompetence(), r.getJustification())));
        assertTrue(filter.process(copy(r, r.getContext(), "x".repeat(min), r.getDistractors(),
                r.getCompetence(), r.getJustification())));
    }

    @Test
    void optionsValidationFilterExigeCuatroOpcionesUnicasYNoVacias() {
        QuestionRequest r = valid();
        OptionsValidationFilter filter = new OptionsValidationFilter();

        assertTrue(filter.process(r));
        assertFalse(filter.process(copy(r, r.getContext(), r.getDirectQuestion(), Arrays.asList("a", "b", "c"),
                r.getCompetence(), r.getJustification())));
        assertFalse(filter.process(copy(r, r.getContext(), r.getDirectQuestion(), Arrays.asList("a", "A", "b", "c"),
                r.getCompetence(), r.getJustification())));
        assertFalse(filter.process(copy(r, r.getContext(), r.getDirectQuestion(), Arrays.asList("a", " ", "b", "c"),
                r.getCompetence(), r.getJustification())));
    }

    @Test
    void classificationFilterExigeCompetenciaTemaSubtemaYNivel() {
        QuestionRequest r = valid();
        assertTrue(new ClassificationFilter().process(r));
        assertFalse(new ClassificationFilter().process(copy(r, r.getContext(), r.getDirectQuestion(),
                r.getDistractors(), "", r.getJustification())));
        QuestionRequest sinNivel = new QuestionRequest(r.getType(), r.getContext(), r.getDirectQuestion(),
                r.getDistractors(), r.getCorrectAnswer(), r.getJustification(), r.getBibliography(),
                r.getCompetence(), r.getTopic(), r.getSubtopic(), null, r.getAuthorLogin());
        assertFalse(new ClassificationFilter().process(sinNivel));
    }

    @Test
    void correctAnswerValidationFilterExigeQueLaRespuestaEsteEnLasOpciones() {
        CorrectAnswerValidationFilter filter = new CorrectAnswerValidationFilter();
        assertTrue(filter.process(valid()));
        assertTrue(filter.process(TestData.withCorrectAnswer(valid(), " microkernel ")));
        assertFalse(filter.process(TestData.withCorrectAnswer(valid(), "Opcion inexistente")));
        assertFalse(filter.process(TestData.withCorrectAnswer(valid(), null)));
    }

    @Test
    void metadataValidationFilterExigeJustificacionBibliografiaYAutor() {
        QuestionRequest r = valid();
        assertTrue(new MetadataValidationFilter().process(r));
        assertFalse(new MetadataValidationFilter().process(copy(r, r.getContext(), r.getDirectQuestion(),
                r.getDistractors(), r.getCompetence(), "")));
    }

    @Test
    void elPipelineSeDetieneEnElPrimerFiltroQueFallaYExponeElMotivo() {
        QuestionPipeline pipeline = fullPipeline();
        QuestionRequest invalido = new QuestionRequest("MULTIPLE_CHOICE", "", "x",
                Collections.emptyList(), "", "", "", "", "", "", DifficultyLevel.BAJO, "");

        assertFalse(pipeline.execute(invalido));
        assertEquals(new ContentValidationFilter().getErrorMessage(), pipeline.getLastError());
    }
}
