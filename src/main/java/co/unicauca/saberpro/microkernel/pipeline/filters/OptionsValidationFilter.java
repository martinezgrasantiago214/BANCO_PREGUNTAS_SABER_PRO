package co.unicauca.saberpro.microkernel.pipeline.filters;

import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Valida que existan exactamente 4 distractores, que ninguno este vacio y
 * que no existan opciones duplicadas (HU01: "Cuatro distractores").
 */
public class OptionsValidationFilter implements QuestionFilter {

    private static final int REQUIRED_OPTIONS = 4;

    @Override
    public boolean process(QuestionRequest request) {
        List<String> options = request.getDistractors();

        if (options == null || options.size() != REQUIRED_OPTIONS) {
            return false;
        }

        Set<String> uniqueOptions = new HashSet<>();
        for (String option : options) {
            if (option == null || option.trim().isEmpty()) {
                return false;
            }
            if (!uniqueOptions.add(option.trim().toLowerCase())) {
                return false; // opcion duplicada
            }
        }
        return true;
    }

    @Override
    public String getErrorMessage() {
        return "Debe indicar exactamente " + REQUIRED_OPTIONS
                + " distractores, sin campos vacios ni valores duplicados.";
    }
}
