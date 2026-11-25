package co.unicauca.saberpro.review.infrastructure.adapter.out.directory;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.review.application.port.out.DirectoryUnavailableException;
import co.unicauca.saberpro.review.application.port.out.ReviewerDirectory;
import co.unicauca.saberpro.review.domain.model.ReviewerCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador de salida del puerto {@link ReviewerDirectory}: consulta el API
 * REST del user-service (GET /api/users/{login}), que a su vez lee los
 * usuarios y roles. En #49 el RestClient recibe el interceptor que
 * agrega el token de servicio (client credentials).
 */
public class UserServiceReviewerDirectory implements ReviewerDirectory {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceReviewerDirectory.class);

    /** Solo los campos del UserResponse del user-service que necesita este servicio. */
    private record UserRole(String login, Role role) {
    }

    private final RestClient restClient;

    public UserServiceReviewerDirectory(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ReviewerCandidate lookup(String login) {
        try {
            UserRole user = restClient.get().uri("/api/users/{login}", login).retrieve().body(UserRole.class);
            return user == null ? ReviewerCandidate.unknown(login) : ReviewerCandidate.of(login, user.role());
        } catch (HttpClientErrorException.NotFound ex) {
            return ReviewerCandidate.unknown(login);
        } catch (RestClientException ex) {
            LOGGER.warn("No fue posible consultar el usuario {} en el user-service: {}", login, ex.getMessage());
            throw new DirectoryUnavailableException("No fue posible verificar el rol de " + login
                    + ": el servicio de usuarios no responde. Intente de nuevo.", ex);
        }
    }
}
