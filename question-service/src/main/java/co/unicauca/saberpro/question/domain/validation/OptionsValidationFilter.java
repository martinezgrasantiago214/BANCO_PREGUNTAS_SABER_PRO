package co.unicauca.saberpro.question.domain.validation;

import co.unicauca.saberpro.question.domain.QuestionRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Valida que existan exactamente 4 distractores, que ninguno este vacio y
 * que no existan opciones duplicadas (HU01: "Cuatro distractores").
 */
public class OptionsValidationFilter implements QuestionFilter {

    public static final int REQUIRED_OPTIONS = 4;

    /** Motivo concreto del ultimo fallo, para que el autor sepa que corregir. */
    private String errorMessage = defaultMessage();

    @Override
    public boolean process(QuestionRequest request) {
        List<String> options = request.getDistractors();
        if (options == null || options.size() != REQUIRED_OPTIONS) {
            errorMessage = "Debe indicar exactamente " + REQUIRED_OPTIONS + " distractores (se recibieron "
                    + (options == null ? 0 : options.size()) + ").";
            return false;
        }
        Set<String> uniqueOptions = new HashSet<>();
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            if (option == null || option.trim().isEmpty()) {
                errorMessage = "El distractor " + (char) ('A' + i) + " esta vacio.";
                return false;
            }
            if (!uniqueOptions.add(option.trim().toLowerCase())) {
                errorMessage = "Los distractores no se pueden repetir: \"" + option.trim() + "\" aparece mas de una vez.";
                return false;
            }
        }
        errorMessage = defaultMessage();
        return true;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    private static String defaultMessage() {
        return "Debe indicar exactamente " + REQUIRED_OPTIONS
                + " distractores, sin campos vacios ni valores duplicados.";
    }
}
