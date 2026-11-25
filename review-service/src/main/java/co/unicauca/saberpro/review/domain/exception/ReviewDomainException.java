package co.unicauca.saberpro.review.domain.exception;

import java.util.List;

/**
 * Base de las excepciones del dominio de revision por pares. El dominio no
 * conoce HTTP: el adaptador web traduce cada subclase a su codigo (400, 403,
 * 404 o 409). Puede llevar varios mensajes, para devolver todos los errores
 * de una vez.
 */
public abstract class ReviewDomainException extends RuntimeException {

    private final List<String> messages;

    protected ReviewDomainException(List<String> messages) {
        super(String.join("; ", messages));
        this.messages = List.copyOf(messages);
    }

    public List<String> getMessages() {
        return messages;
    }
}
