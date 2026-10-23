package co.unicauca.saberpro.frontend.client;

import co.unicauca.saberpro.common.contract.ApiError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Cliente HTTP/JSON hacia el API Gateway. Es el unico lugar del frontend que
 * conoce HTTP; las pantallas no lo usan directamente (ver BancoPreguntasFacade).
 */
public class ApiClient {

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public <T> T get(String path, TypeReference<T> responseType) {
        return send("GET", path, null, responseType);
    }

    public <T> T post(String path, Object body, TypeReference<T> responseType) {
        return send("POST", path, body, responseType);
    }

    public <T> T put(String path, Object body, TypeReference<T> responseType) {
        return send("PUT", path, body, responseType);
    }

    public <T> T patch(String path, Object body, TypeReference<T> responseType) {
        return send("PATCH", path, body, responseType);
    }

    private <T> T send(String method, String path, Object body, TypeReference<T> responseType) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(toJson(body), StandardCharsets.UTF_8));
        }

        HttpResponse<String> response;
        try {
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new ApiException(0, List.of("No fue posible conectar con el API Gateway en " + baseUrl
                    + ". Verifique que los servicios esten en ejecucion."));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, List.of("La peticion fue interrumpida."));
        }

        if (response.statusCode() >= 400) {
            throw toApiException(response);
        }
        try {
            return mapper.readValue(response.body(), responseType);
        } catch (JsonProcessingException ex) {
            throw new ApiException(response.statusCode(), List.of("Respuesta inesperada del servidor: " + ex.getOriginalMessage()));
        }
    }

    private String toJson(Object body) {
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("No fue posible convertir la peticion a JSON", ex);
        }
    }

    private ApiException toApiException(HttpResponse<String> response) {
        try {
            ApiError error = mapper.readValue(response.body(), ApiError.class);
            if (error.messages() != null && !error.messages().isEmpty()) {
                return new ApiException(response.statusCode(), error.messages());
            }
        } catch (JsonProcessingException ignored) {
            // el cuerpo no es un ApiError (p. ej. error del gateway): se usa el codigo HTTP
        }
        return new ApiException(response.statusCode(), List.of("El servidor respondio con el codigo "
                + response.statusCode() + "."));
    }
}
