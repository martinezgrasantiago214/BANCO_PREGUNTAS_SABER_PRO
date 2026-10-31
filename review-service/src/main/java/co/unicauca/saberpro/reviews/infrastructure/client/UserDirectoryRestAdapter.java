package co.unicauca.saberpro.reviews.infrastructure.client;

import co.unicauca.saberpro.reviews.application.port.UserDirectoryPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Adaptador REST hacia user-service. La URL usa el nombre logico del
 * servicio: el RestClient @LoadBalanced lo resuelve con Eureka y reparte
 * las llamadas entre las instancias disponibles.
 */
@Component
public class UserDirectoryRestAdapter implements UserDirectoryPort {

    private final RestClient restClient;

    public UserDirectoryRestAdapter(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.baseUrl("http://user-service").build();
    }

    @Override
    public boolean hasActiveRole(String login, String role) {
        if (login == null || login.isBlank()) {
            return false;
        }
        try {
            UserView user = restClient.get().uri("/api/users/{login}", login).retrieve().body(UserView.class);
            return user != null && role.equals(user.role()) && "ACTIVO".equals(user.status());
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(404))) {
                return false;
            }
            throw ex;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserView(String login, String role, String status) {
    }
}
