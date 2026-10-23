package co.unicauca.saberpro.common.web;

import java.util.List;

/** Regla de negocio incumplida; se responde con 400 Bad Request. */
public class BusinessException extends RuntimeException {

    private final List<String> messages;

    public BusinessException(String message) {
        this(List.of(message));
    }

    public BusinessException(List<String> messages) {
        super(String.join("; ", messages));
        this.messages = List.copyOf(messages);
    }

    public List<String> getMessages() {
        return messages;
    }
}
