package co.unicauca.saberpro.question.domain.factory;

import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.QuestionRequest;
import co.unicauca.saberpro.question.domain.validation.ClassificationFilter;
import co.unicauca.saberpro.question.domain.validation.ContentValidationFilter;
import co.unicauca.saberpro.question.domain.validation.QuestionPipeline;

/** Preguntas basadas en el analisis de un caso o escenario extenso. */
public class CaseQuestionFactory extends QuestionFactory {

    public static final String TYPE = "CASO";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getName() {
        return "Analisis de caso";
    }

    @Override
    protected Question createQuestion(QuestionRequest request) {
        return baseBuilder(request)
                .context("[Analisis de caso] " + request.getContext())
                .build();
    }

    @Override
    public QuestionPipeline createValidationPipeline() {
        return new QuestionPipeline()
                .addFilter(new ContentValidationFilter())
                .addFilter(new ClassificationFilter());
    }
}
