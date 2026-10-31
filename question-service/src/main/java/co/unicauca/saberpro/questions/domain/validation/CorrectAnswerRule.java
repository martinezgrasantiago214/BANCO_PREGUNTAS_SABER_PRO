package co.unicauca.saberpro.questions.domain.validation;

import co.unicauca.saberpro.questions.domain.model.QuestionData;

import java.util.Optional;

/** La respuesta correcta existe y coincide con una de las opciones (unica respuesta). */
public class CorrectAnswerRule extends QuestionValidationHandler {

    @Override
    protected Optional<String> check(QuestionData d) {
        if (isBlank(d.correctAnswer())) {
            return Optional.of("Debe indicar la respuesta correcta.");
        }
        long matches = d.options().stream()
                .filter(o -> o != null && o.trim().equalsIgnoreCase(d.correctAnswer().trim()))
                .count();
        if (matches != 1) {
            return Optional.of("La respuesta correcta debe coincidir exactamente con una de las opciones.");
        }
        return Optional.empty();
    }
}
