package co.unicauca.saberpro.review.application.port.out;

import co.unicauca.saberpro.review.domain.model.ReviewerCandidate;

/**
 * Puerto de salida: consulta quien es un usuario y que rol tiene. Lo
 * implementa un adaptador que llama al user-service (que a su vez consulta
 * Keycloak); el caso de uso no sabe nada de HTTP.
 */
public interface ReviewerDirectory {

    /**
     * @return el candidato con su rol, o {@link ReviewerCandidate#unknown} si no existe.
     * @throws co.unicauca.saberpro.review.domain.exception.ReviewConflictException si el directorio no responde.
     */
    ReviewerCandidate lookup(String login);
}
