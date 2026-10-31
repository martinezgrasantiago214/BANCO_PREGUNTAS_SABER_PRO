package co.unicauca.saberpro.client.api.http;

import java.util.List;

/** Error devuelto por la API (o de conexion), con el detalle para mostrar al usuario. */
public class ApiException extends RuntimeException {

    private final int status;
    private final List<String> details;

    public ApiException(int status, String message, List<String> details) {
        super(message);
        this.status = status;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public int getStatus() { return status; }
    public List<String> getDetails() { return details; }

    /** Mensaje listo para un dialogo: mensaje principal + vinetas de detalle. */
    public String toUserMessage() {
        StringBuilder sb = new StringBuilder(getMessage());
        details.forEach(d -> sb.append("\n • ").append(d));
        return sb.toString();
    }
}
