package co.unicauca.saberpro.microkernel.plugins;

import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.microkernel.common.QuestionPlugin;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionPipeline;
import co.unicauca.saberpro.microkernel.pipeline.filters.*;

import java.util.UUID;

/**
 * Plugin "GeneradorPreguntaSeleccionMultiple": genera preguntas de seleccion
 * multiple con unica respuesta (HU01). Es el plugin principal exigido por
 * el taller: integra el pipeline de Tuberias y Filtros COMPLETO (los 5
 * filtros de validacion estructural, HU03) antes de aceptar la pregunta.
 */
public class MultipleChoiceQuestionPlugin implements QuestionPlugin {

    public static final String TYPE = "MULTIPLE_CHOICE";

    private final QuestionPipeline pipeline;
    private String lastValidationError;

    public MultipleChoiceQuestionPlugin() {
        pipeline = new QuestionPipeline()
                .addFilter(new ContentValidationFilter())
                .addFilter(new OptionsValidationFilter())
                .addFilter(new ClassificationFilter())
                .addFilter(new CorrectAnswerValidationFilter())
                .addFilter(new MetadataValidationFilter());
    }

    @Override
    public String getName() {
        return "GeneradorPreguntaSeleccionMultiple";
    }

    @Override
    public boolean supports(String type) {
        return TYPE.equalsIgnoreCase(type);
    }

    @Override
    public Question generate(QuestionRequest request) {
        if (!pipeline.execute(request)) {
            lastValidationError = pipeline.getLastError();
            return null;
        }
        return new Question(
                UUID.randomUUID().toString(),
                TYPE,
                request.getContext(),
                request.getDirectQuestion(),
                request.getDistractors(),
                request.getCorrectAnswer(),
                request.getJustification(),
                request.getBibliography(),
                request.getCompetence(),
                request.getTopic(),
                request.getSubtopic(),
                request.getDifficultyLevel(),
                request.getAuthorLogin()
        );
    }

    @Override
    public String getLastValidationError() {
        return lastValidationError;
    }
}
