package co.unicauca.saberpro.microkernel.plugins;

import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.microkernel.common.QuestionPlugin;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionPipeline;
import co.unicauca.saberpro.microkernel.pipeline.filters.ContentValidationFilter;
import co.unicauca.saberpro.microkernel.pipeline.filters.ClassificationFilter;

import java.util.UUID;

/**
 * Plugin "GeneradorPreguntaCaso": genera preguntas basadas en analisis de
 * caso / escenario extenso. Demuestra la extensibilidad del microkernel:
 * la institucion puede agregar nuevos tipos de pregunta sin modificar el
 * nucleo (Open/Closed Principle a nivel de arquitectura).
 */
public class CaseQuestionPlugin implements QuestionPlugin {

    public static final String TYPE = "CASO";

    private final QuestionPipeline pipeline;
    private String lastValidationError;

    public CaseQuestionPlugin() {
        pipeline = new QuestionPipeline()
                .addFilter(new ContentValidationFilter())
                .addFilter(new ClassificationFilter());
    }

    @Override
    public String getName() {
        return "GeneradorPreguntaCaso";
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
                "[Analisis de caso] " + request.getContext(),
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
