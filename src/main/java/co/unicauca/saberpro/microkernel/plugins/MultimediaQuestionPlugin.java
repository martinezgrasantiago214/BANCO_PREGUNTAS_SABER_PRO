package co.unicauca.saberpro.microkernel.plugins;

import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.microkernel.common.QuestionPlugin;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionPipeline;
import co.unicauca.saberpro.microkernel.pipeline.filters.ContentValidationFilter;

import java.util.UUID;

/**
 * Plugin "GeneradorPreguntaMultimedia": genera preguntas que referencian
 * recursos multimedia (imagenes, audio o video) dentro del contenido.
 */
public class MultimediaQuestionPlugin implements QuestionPlugin {

    public static final String TYPE = "MULTIMEDIA";

    private final QuestionPipeline pipeline;
    private String lastValidationError;

    public MultimediaQuestionPlugin() {
        pipeline = new QuestionPipeline()
                .addFilter(new ContentValidationFilter());
    }

    @Override
    public String getName() {
        return "GeneradorPreguntaMultimedia";
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
                "[Recurso multimedia adjunto] " + request.getContext(),
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
