package co.unicauca.saberpro.common.web;

/**
 * El usuario no tiene permiso para la operacion (por ejemplo, editar una
 * pregunta que no es suya); se responde con 403 Forbidden.
 */
public class ForbiddenOperationException extends BusinessException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
