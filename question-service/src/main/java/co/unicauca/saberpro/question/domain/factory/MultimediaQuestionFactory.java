package co.unicauca.saberpro.question.domain.factory;

import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.QuestionRequest;
import co.unicauca.saberpro.question.domain.validation.ContentValidationFilter;
import co.unicauca.saberpro.question.domain.validation.QuestionPipeline;

/** Preguntas que referencian recursos multimedia (imagen, audio o video). */
public class MultimediaQuestionFactory extends QuestionFactory {

    public static final String TYPE = "MULTIMEDIA";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getName() {
        return "Pregunta con recurso multimedia";
    }

    @Override
    protected Question createQuestion(QuestionRequest request) {
        return baseBuilder(request)
                .context("[Recurso multimedia adjunto] " + request.getContext())
                .build();
    }

    @Override
    public QuestionPipeline createValidationPipeline() {
        return new QuestionPipeline()
                .addFilter(new ContentValidationFilter());
    }
}
