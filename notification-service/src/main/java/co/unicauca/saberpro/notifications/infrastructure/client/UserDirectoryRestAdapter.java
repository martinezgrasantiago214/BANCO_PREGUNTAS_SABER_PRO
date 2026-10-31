package co.unicauca.saberpro.notifications.infrastructure.client;

import co.unicauca.saberpro.notifications.application.port.UserDirectoryPort;
import co.unicauca.saberpro.notifications.domain.Recipient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

/** Adaptador REST (balanceado por Eureka) hacia user-service. */
@Component
public class UserDirectoryRestAdapter implements UserDirectoryPort {

    private final RestClient restClient;

    public UserDirectoryRestAdapter(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.baseUrl("http://user-service").build();
    }

    @Override
    public Optional<Recipient> findByLogin(String login) {
        try {
            UserView u = restClient.get().uri("/api/users/{login}", login).retrieve().body(UserView.class);
            return Optional.ofNullable(u).filter(UserView::active).map(UserView::toRecipient);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<Recipient> findByRole(String role) {
        List<UserView> users = restClient.get().uri("/api/users?role={role}", role).retrieve()
                .body(new ParameterizedTypeReference<List<UserView>>() { });
        return users == null ? List.of() : users.stream().filter(UserView::active).map(UserView::toRecipient).toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserView(String login, String fullName, String email, String status) {
        boolean active() {
            return "ACTIVO".equals(status);
        }

        Recipient toRecipient() {
            return new Recipient(login, fullName, email);
        }
    }
}
