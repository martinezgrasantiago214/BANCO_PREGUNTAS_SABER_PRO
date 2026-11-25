package co.unicauca.saberpro.review.application.port.out;

/**
 * El directorio de usuarios no respondio (fallo tecnico, no de negocio).
 * El adaptador web lo traduce a 503 Service Unavailable: el administrador
 * puede reintentar la asignacion.
 */
public class DirectoryUnavailableException extends RuntimeException {

    public DirectoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
