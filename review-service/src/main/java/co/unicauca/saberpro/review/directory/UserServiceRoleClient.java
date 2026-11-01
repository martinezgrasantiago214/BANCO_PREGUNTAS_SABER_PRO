package co.unicauca.saberpro.review.directory;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.common.web.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Implementacion de {@link UserRoleDirectory} que consulta el API REST del
 * user-service (GET /api/users/{login}); el review-service nunca lee la base
 * de datos de usuarios.
 */
public class UserServiceRoleClient implements UserRoleDirectory {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceRoleClient.class);

    /** Solo los campos del UserResponse del user-service que necesita este servicio. */
    private record UserRole(String login, Role role) {
    }

    private final RestClient restClient;

    public UserServiceRoleClient(String userServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
    }

    @Override
    public Optional<Role> findRole(String login) {
        try {
            UserRole user = restClient.get().uri("/api/users/{login}", login).retrieve().body(UserRole.class);
            return Optional.ofNullable(user).map(UserRole::role);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            LOGGER.warn("No fue posible consultar el usuario {} en el user-service: {}", login, ex.getMessage());
            throw new BusinessException("No fue posible verificar el rol de " + login
                    + ": el servicio de usuarios no responde. Intente de nuevo.");
        }
    }
}
