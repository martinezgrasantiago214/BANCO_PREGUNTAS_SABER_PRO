package co.unicauca.saberpro.review.domain.exception;

import java.util.List;

/**
 * Bloqueo optimista: otra peticion modifico la misma revision entre la
 * lectura y la escritura (p. ej. dos administradores asignando a la vez, o
 * dos instancias del servicio). Nada se guardo; basta con reintentar.
 */
public class ConcurrentReviewUpdateException extends ReviewDomainException {

    public ConcurrentReviewUpdateException(String questionId) {
        super(List.of("La revision de la pregunta " + questionId
                + " fue modificada por otra operacion al mismo tiempo. Actualice e intente de nuevo."));
    }
}
