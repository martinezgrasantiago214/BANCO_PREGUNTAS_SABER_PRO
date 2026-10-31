package co.unicauca.saberpro.questions.domain.plugin;

import co.unicauca.saberpro.questions.domain.model.Question;
import co.unicauca.saberpro.questions.domain.model.QuestionData;

import java.util.UUID;

/** Creador concreto: preguntas de seleccion multiple con unica respuesta (HU01). */
public class MultipleChoiceQuestionPlugin extends QuestionTypePlugin {

    public static final String TYPE = "MULTIPLE_CHOICE";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String displayName() {
        return "Selección múltiple con única respuesta";
    }

    @Override
    protected Question createQuestion(QuestionData data) {
        return Question.builder()
                .id(UUID.randomUUID().toString())
                .type(TYPE)
                .authorLogin(data.authorLogin().trim())
                .context(data.context().trim())
                .statement(data.statement().trim())
                .options(buildOptions(data))
                .justification(data.justification().trim())
                .bibliography(data.bibliography().trim())
                .competence(data.competence().trim())
                .topic(data.topic().trim())
                .subtopic(data.subtopic().trim())
                .difficulty(data.difficulty())
                .build();
    }
}
