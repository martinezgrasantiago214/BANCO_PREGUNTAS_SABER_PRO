package co.unicauca.saberpro.common.web;

/**
 * La operacion no es posible en el estado actual del recurso (por ejemplo,
 * editar una pregunta que ya no esta en Borrador); se responde con 409 Conflict.
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(message);
    }
}
