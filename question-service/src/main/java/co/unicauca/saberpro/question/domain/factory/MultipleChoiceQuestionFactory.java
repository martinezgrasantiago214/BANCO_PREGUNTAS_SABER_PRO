package co.unicauca.saberpro.question.domain.factory;

import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.QuestionRequest;
import co.unicauca.saberpro.question.domain.validation.ClassificationFilter;
import co.unicauca.saberpro.question.domain.validation.ContentValidationFilter;
import co.unicauca.saberpro.question.domain.validation.CorrectAnswerValidationFilter;
import co.unicauca.saberpro.question.domain.validation.MetadataValidationFilter;
import co.unicauca.saberpro.question.domain.validation.OptionsValidationFilter;
import co.unicauca.saberpro.question.domain.validation.QuestionPipeline;

/**
 * Fabrica principal (HU01): preguntas de seleccion multiple con unica
 * respuesta, con la validacion estructural completa de 5 filtros.
 */
public class MultipleChoiceQuestionFactory extends QuestionFactory {

    public static final String TYPE = "MULTIPLE_CHOICE";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getName() {
        return "Seleccion multiple con unica respuesta";
    }

    @Override
    protected Question createQuestion(QuestionRequest request) {
        return baseBuilder(request).build();
    }

    @Override
    public QuestionPipeline createValidationPipeline() {
        return new QuestionPipeline()
                .addFilter(new ContentValidationFilter())
                .addFilter(new OptionsValidationFilter())
                .addFilter(new ClassificationFilter())
                .addFilter(new CorrectAnswerValidationFilter())
                .addFilter(new MetadataValidationFilter());
    }
}
