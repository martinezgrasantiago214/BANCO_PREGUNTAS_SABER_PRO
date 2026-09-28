package co.unicauca.saberpro.service;

import java.util.Collections;
import java.util.List;

/**
 * Resultado generico de una operacion del servicio: indica si fue exitosa y,
 * en caso contrario, la lista de errores para mostrar al usuario.
 */
public class OperationResult {

    private final boolean success;
    private final List<String> errors;
    /** Dato opcional devuelto por una operacion exitosa (p. ej. la pregunta creada). */
    private final Object data;

    private OperationResult(boolean success, List<String> errors) {
        this(success, errors, null);
    }

    private OperationResult(boolean success, List<String> errors, Object data) {
        this.success = success;
        this.errors = errors;
        this.data = data;
    }

    public static OperationResult ok() {
        return new OperationResult(true, Collections.emptyList());
    }

    /** Operacion exitosa que ademas devuelve un dato (usado por la API REST). */
    public static OperationResult ok(Object data) {
        return new OperationResult(true, Collections.emptyList(), data);
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

    /** @return el dato asociado a la operacion exitosa, o {@code null}. */
    public Object getData() {
        return data;
    }

    public String getErrorsAsText() {
        return String.join("\n", errors);
    }
}
