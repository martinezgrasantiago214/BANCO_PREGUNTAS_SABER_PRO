package co.unicauca.saberpro.questions.domain.validation;

import co.unicauca.saberpro.questions.domain.model.QuestionData;

import java.util.Optional;

/** Contexto obligatorio y pregunta directa de al menos 10 caracteres. */
public class ContentRule extends QuestionValidationHandler {

    static final int MIN_STATEMENT_LENGTH = 10;

    @Override
    protected Optional<String> check(QuestionData d) {
        if (isBlank(d.context())) {
            return Optional.of("El contexto es obligatorio.");
        }
        if (isBlank(d.statement()) || d.statement().trim().length() < MIN_STATEMENT_LENGTH) {
            return Optional.of("La pregunta directa debe tener al menos " + MIN_STATEMENT_LENGTH + " caracteres.");
        }
        return Optional.empty();
    }
}
