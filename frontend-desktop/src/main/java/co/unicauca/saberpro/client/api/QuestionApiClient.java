package co.unicauca.saberpro.client.api;

import co.unicauca.saberpro.client.api.dto.CatalogItemDto;
import co.unicauca.saberpro.client.api.dto.PageDto;
import co.unicauca.saberpro.client.api.dto.QuestionDto;
import co.unicauca.saberpro.client.api.dto.QuestionRequestDto;
import co.unicauca.saberpro.client.api.http.JsonHttpClient;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

/** Cliente de question-service (rutas /api/questions). */
public class QuestionApiClient {

    private final JsonHttpClient http;

    public QuestionApiClient(JsonHttpClient http) {
        this.http = http;
    }

    public QuestionDto create(QuestionRequestDto body) {
        return http.post("/api/questions", body, new TypeReference<>() { });
    }

    public QuestionDto update(String id, QuestionRequestDto body) {
        return http.put("/api/questions/" + id, body, new TypeReference<>() { });
    }

    public void delete(String id) {
        http.delete("/api/questions/" + id);
    }

    public QuestionDto get(String id) {
        return http.get("/api/questions/" + id, Map.of(), new TypeReference<>() { });
    }

    public QuestionDto submitForReview(String id) {
        return http.post("/api/questions/" + id + "/submit-for-review", null, new TypeReference<>() { });
    }

    public PageDto<QuestionDto> search(Map<String, ?> filters) {
        return http.get("/api/questions", filters, new TypeReference<>() { });
    }

    public List<CatalogItemDto> states() {
        return http.get("/api/questions/catalog/states", Map.of(), new TypeReference<>() { });
    }
}
