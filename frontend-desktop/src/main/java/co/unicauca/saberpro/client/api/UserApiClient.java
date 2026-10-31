package co.unicauca.saberpro.client.api;

import co.unicauca.saberpro.client.api.dto.UserDto;
import co.unicauca.saberpro.client.api.http.JsonHttpClient;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

/** Cliente de user-service (rutas /api/auth y /api/users). */
public class UserApiClient {

    private final JsonHttpClient http;

    public UserApiClient(JsonHttpClient http) {
        this.http = http;
    }

    public UserDto login(String login, String password) {
        return http.post("/api/auth/login", Map.of("login", login, "password", password), new TypeReference<>() { });
    }

    public UserDto register(String login, String fullName, String email, String role, String password) {
        return http.post("/api/users", Map.of("login", login, "fullName", fullName, "email", email,
                "role", role, "password", password), new TypeReference<>() { });
    }

    public List<UserDto> byRole(String role) {
        return http.get("/api/users", Map.of("role", role), new TypeReference<>() { });
    }
}
