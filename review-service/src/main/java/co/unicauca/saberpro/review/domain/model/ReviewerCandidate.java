package co.unicauca.saberpro.review.domain.model;

import co.unicauca.saberpro.common.contract.Role;

import java.util.Objects;

/**
 * Objeto de valor: un usuario que el administrador quiere asignar como
 * revisor, con lo que el directorio de usuarios (Keycloak, via user-service)
 * dice de el. El agregado decide si es elegible; asi la regla "solo usuarios
 * con rol Revisor" vive en el dominio y no en el adaptador.
 */
public record ReviewerCandidate(String login, boolean exists, Role role) {

    public ReviewerCandidate {
        Objects.requireNonNull(login, "login");
    }

    public static ReviewerCandidate of(String login, Role role) {
        return new ReviewerCandidate(login, true, role);
    }

    public static ReviewerCandidate unknown(String login) {
        return new ReviewerCandidate(login, false, null);
    }

    public boolean isReviewer() {
        return exists && role == Role.REVISOR;
    }
}
