package co.unicauca.saberpro.user.service;

import java.util.Collections;
import java.util.List;

/**
 * Resultado generico de una operacion del servicio: indica si fue exitosa y,
 * en caso contrario, la lista de errores para mostrar al usuario.
 */
public class OperationResult {

    private final boolean success;
    private final List<String> errors;

    private OperationResult(boolean success, List<String> errors) {
        this.success = success;
        this.errors = errors;
    }

    public static OperationResult ok() {
        return new OperationResult(true, Collections.emptyList());
    }

    public static OperationResult fail(List<String> errors) {
        return new OperationResult(false, errors);
    }

    public static OperationResult fail(String error) {
        return new OperationResult(false, Collections.singletonList(error));
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getErrorsAsText() {
        return String.join("\n", errors);
    }
}
