package co.unicauca.saberpro.client.api;

import co.unicauca.saberpro.client.api.dto.NotificationDto;
import co.unicauca.saberpro.client.api.http.JsonHttpClient;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

/** Cliente de notification-service (rutas /api/notifications). */
public class NotificationApiClient {

    private final JsonHttpClient http;

    public NotificationApiClient(JsonHttpClient http) {
        this.http = http;
    }

    public NotificationDto.Page inbox(int page, int size) {
        return http.get("/api/notifications", Map.of("page", page, "size", size), new TypeReference<>() { });
    }

    public NotificationDto markRead(long id) {
        return http.patch("/api/notifications/" + id + "/read", null, new TypeReference<>() { });
    }
}
