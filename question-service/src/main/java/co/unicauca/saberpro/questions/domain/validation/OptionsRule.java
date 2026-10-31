package co.unicauca.saberpro.questions.domain.validation;

import co.unicauca.saberpro.questions.domain.model.QuestionData;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Exactamente 4 opciones, sin vacias ni duplicadas. */
public class OptionsRule extends QuestionValidationHandler {

    static final int REQUIRED_OPTIONS = 4;

    @Override
    protected Optional<String> check(QuestionData d) {
        if (d.options().size() != REQUIRED_OPTIONS) {
            return Optional.of("Debe registrar exactamente " + REQUIRED_OPTIONS + " opciones de respuesta.");
        }
        Set<String> unique = new HashSet<>();
        for (String option : d.options()) {
            if (isBlank(option)) {
                return Optional.of("Ninguna opción de respuesta puede estar vacía.");
            }
            if (!unique.add(option.trim().toLowerCase())) {
                return Optional.of("Las opciones de respuesta no pueden repetirse.");
            }
        }
        return Optional.empty();
    }
}
