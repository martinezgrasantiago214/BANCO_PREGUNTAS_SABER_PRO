package co.unicauca.saberpro.common.web;

/** El recurso solicitado no existe; se responde con 404 Not Found. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
