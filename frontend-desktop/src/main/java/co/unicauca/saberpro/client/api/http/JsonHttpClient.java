package co.unicauca.saberpro.client.api.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Supplier;

/**
 * Cliente HTTP/JSON de bajo nivel. Agrega el encabezado X-User-Login con el
 * usuario de la sesion y traduce las respuestas de error a ApiException.
 */
public class JsonHttpClient {

    private static final String USER_HEADER = "X-User-Login";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final String baseUrl;
    private final Supplier<String> currentUser;

    public JsonHttpClient(String baseUrl, Supplier<String> currentUser) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.currentUser = currentUser;
    }

    public <T> T get(String path, Map<String, ?> query, TypeReference<T> type) {
        return send(request(path + queryString(query)).GET(), type);
    }

    public <T> T post(String path, Object body, TypeReference<T> type) {
        return send(request(path).POST(bodyOf(body)), type);
    }

    public <T> T put(String path, Object body, TypeReference<T> type) {
        return send(request(path).PUT(bodyOf(body)), type);
    }

    public <T> T patch(String path, Object body, TypeReference<T> type) {
        return send(request(path).method("PATCH", bodyOf(body)), type);
    }

    public void delete(String path) {
        send(request(path).DELETE(), null);
    }

    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        String user = currentUser.get();
        if (user != null) {
            b.header(USER_HEADER, user);
        }
        return b;
    }

    private HttpRequest.BodyPublisher bodyOf(Object body) {
        try {
            return body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApiException(0, "No se pudo serializar la solicitud.", List.of(e.getMessage()));
        }
    }

    private <T> T send(HttpRequest.Builder builder, TypeReference<T> type) {
        try {
            HttpResponse<String> response = http.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw toApiException(response);
            }
            if (type == null || response.body() == null || response.body().isBlank()) {
                return null;
            }
            return mapper.readValue(response.body(), type);
        } catch (ConnectException e) {
            throw new ApiException(0, "No hay conexión con el API Gateway (" + baseUrl
                    + "). ¿Están levantados los microservicios?", List.of());
        } catch (IOException e) {
            throw new ApiException(0, "Error de comunicación con el servidor.", List.of(String.valueOf(e.getMessage())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "La operación fue interrumpida.", List.of());
        }
    }

    private ApiException toApiException(HttpResponse<String> response) {
        String message = "Error " + response.statusCode();
        List<String> details = new ArrayList<>();
        try {
            JsonNode node = mapper.readTree(response.body());
            if (node.hasNonNull("message")) {
                message = node.get("message").asText();
            }
            if (node.has("details") && node.get("details").isArray()) {
                node.get("details").forEach(d -> details.add(d.asText()));
            }
        } catch (IOException | RuntimeException ignored) {
            if (response.statusCode() == 503) {
                message = "El servicio no está disponible en este momento.";
            }
        }
        return new ApiException(response.statusCode(), message, details);
    }

    private static String queryString(Map<String, ?> query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("&", "?", "");
        query.forEach((k, v) -> {
            if (v != null && !String.valueOf(v).isBlank()) {
                joiner.add(k + "=" + URLEncoder.encode(String.valueOf(v), StandardCharsets.UTF_8));
            }
        });
        return joiner.length() == 1 ? "" : joiner.toString();
    }
}
