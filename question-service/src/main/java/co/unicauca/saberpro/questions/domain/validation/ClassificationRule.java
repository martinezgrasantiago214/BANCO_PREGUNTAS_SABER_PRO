package co.unicauca.saberpro.questions.domain.validation;

import co.unicauca.saberpro.questions.domain.model.QuestionData;

import java.util.Optional;

/** Competencia, Tema, Subtema y Nivel de dificultad son obligatorios. */
public class ClassificationRule extends QuestionValidationHandler {

    @Override
    protected Optional<String> check(QuestionData d) {
        if (isBlank(d.competence()) || isBlank(d.topic()) || isBlank(d.subtopic()) || d.difficulty() == null) {
            return Optional.of("Debe indicar Competencia, Tema, Subtema y Nivel de dificultad.");
        }
        return Optional.empty();
    }
}
