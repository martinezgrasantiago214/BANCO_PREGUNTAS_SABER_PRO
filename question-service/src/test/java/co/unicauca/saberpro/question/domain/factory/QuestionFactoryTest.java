package co.unicauca.saberpro.question.domain.factory;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.question.TestData;
import co.unicauca.saberpro.question.domain.Question;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Patron Factory Method: cada fabrica decide que pregunta construye y como la valida. */
class QuestionFactoryTest {

    @Test
    void laFabricaDeSeleccionMultipleCreaUnaPreguntaValidaEnBorrador() {
        QuestionFactory factory = new MultipleChoiceQuestionFactory();
        Question q = factory.create(TestData.validRequest(MultipleChoiceQuestionFactory.TYPE));

        assertEquals(MultipleChoiceQuestionFactory.TYPE, q.getType());
        assertEquals(QuestionStatus.BORRADOR, q.getStatus());
        assertEquals(5, factory.createValidationPipeline().size());
    }

    @Test
    void laFabricaRechazaUnaSolicitudQueNoSuperaLaValidacion() {
        QuestionFactory factory = new MultipleChoiceQuestionFactory();

        QuestionValidationException ex = assertThrows(QuestionValidationException.class, () ->
                factory.create(TestData.withCorrectAnswer(TestData.validRequest("x"), "No esta en las opciones")));
        assertTrue(ex.getMessage().contains("respuesta correcta"));
    }

    @Test
    void lasFabricasDeCasoYMultimediaPersonalizanElContexto() {
        Question caso = new CaseQuestionFactory().create(TestData.validRequest(CaseQuestionFactory.TYPE));
        Question multimedia = new MultimediaQuestionFactory().create(TestData.validRequest("MULTIMEDIA"));

        assertEquals(CaseQuestionFactory.TYPE, caso.getType());
        assertTrue(caso.getContext().startsWith("[Analisis de caso]"));
        assertEquals(MultimediaQuestionFactory.TYPE, multimedia.getType());
        assertTrue(multimedia.getContext().startsWith("[Recurso multimedia adjunto]"));
    }

    @Test
    void cadaFabricaSoportaSoloSuTipoSinImportarMayusculas() {
        QuestionFactory factory = new CaseQuestionFactory();
        assertTrue(factory.supports("caso"));
        assertFalse(factory.supports(MultipleChoiceQuestionFactory.TYPE));
        assertFalse(factory.getName().isBlank());
    }

    @Test
    void elRegistroCargaLasTresFabricasPorReflexion() {
        QuestionFactoryRegistry registry = new QuestionFactoryRegistry();
        List<QuestionFactory> all = registry.all();

        assertEquals(3, all.size());
        assertInstanceOf(MultipleChoiceQuestionFactory.class, registry.forType("MULTIPLE_CHOICE").orElseThrow());
        assertInstanceOf(MultimediaQuestionFactory.class, registry.forType("multimedia").orElseThrow());
        assertTrue(registry.forType("VERDADERO_FALSO").isEmpty());
    }

    @Test
    void unArchivoDeConfiguracionInexistenteDejaElRegistroVacio() {
        assertTrue(new QuestionFactoryRegistry("no-existe.properties").all().isEmpty());
    }
}
