package co.unicauca.saberpro.frontend.client;

import java.util.List;

/** Error devuelto por el backend (o de conexion, con status 0), con los mensajes para el usuario. */
public class ApiException extends RuntimeException {

    public static final int NOT_IMPLEMENTED = 501;

    private final int status;
    private final List<String> messages;

    public ApiException(int status, List<String> messages) {
        super(String.join("\n", messages));
        this.status = status;
        this.messages = List.copyOf(messages);
    }

    public int getStatus() {
        return status;
    }

    public List<String> getMessages() {
        return messages;
    }

    /** El caso de uso todavia no esta implementado en el backend (responde 501 con su issue de Jira). */
    public boolean isPendingFeature() {
        return status == NOT_IMPLEMENTED;
    }
}
