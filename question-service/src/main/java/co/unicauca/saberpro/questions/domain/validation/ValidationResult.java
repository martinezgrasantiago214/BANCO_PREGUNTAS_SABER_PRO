package co.unicauca.saberpro.questions.domain.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Acumula los errores encontrados por los eslabones de la cadena. */
public class ValidationResult {

    private final List<String> errors = new ArrayList<>();

    public void addError(String error) {
        errors.add(error);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
