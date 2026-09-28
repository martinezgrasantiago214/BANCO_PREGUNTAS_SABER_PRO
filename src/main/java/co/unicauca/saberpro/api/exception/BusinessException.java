package co.unicauca.saberpro.api.exception;

import java.util.List;

/**
 * Error de regla de negocio o de validacion (HTTP 400). Envuelve los
 * mensajes de OperationResult que produce la capa de servicio (por ejemplo
 * el filtro del pipeline que rechazo la pregunta).
 */
public class BusinessException extends RuntimeException {

    private final List<String> messages;

    public BusinessException(List<String> messages) {
        super(String.join("; ", messages));
        this.messages = messages;
    }

    public BusinessException(String message) {
        this(List.of(message));
    }

    public List<String> getMessages() {
        return messages;
    }
}
