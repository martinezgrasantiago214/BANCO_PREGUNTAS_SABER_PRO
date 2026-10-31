package co.unicauca.saberpro.client.api;

import co.unicauca.saberpro.client.api.dto.*;
import co.unicauca.saberpro.client.api.http.JsonHttpClient;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cliente de review-service (rutas /api/reviews). */
public class ReviewApiClient {

    private final JsonHttpClient http;

    public ReviewApiClient(JsonHttpClient http) {
        this.http = http;
    }

    public PageDto<ReviewDto> pendingAssignment(int page, int size) {
        return http.get("/api/reviews/pending-assignment", Map.of("page", page, "size", size), new TypeReference<>() { });
    }

    public ReviewDto assignReviewers(String questionId, List<String> reviewerLogins) {
        return http.post("/api/reviews/" + questionId + "/reviewers", Map.of("reviewerLogins", reviewerLogins),
                new TypeReference<>() { });
    }

    public PageDto<ReviewDto> assignedToMe(String status, int page, int size) {
        Map<String, Object> q = new HashMap<>();
        q.put("status", status);
        q.put("page", page);
        q.put("size", size);
        return http.get("/api/reviews/assigned", q, new TypeReference<>() { });
    }

    public ReviewDto get(String questionId) {
        return http.get("/api/reviews/" + questionId, Map.of(), new TypeReference<>() { });
    }

    public ReviewDto evaluate(String questionId, EvaluationRequestDto body) {
        return http.post("/api/reviews/" + questionId + "/evaluations", body, new TypeReference<>() { });
    }

    public ObservationDto observe(String questionId, String text) {
        return http.post("/api/reviews/" + questionId + "/observations", Map.of("text", text), new TypeReference<>() { });
    }

    public List<ObservationDto> observations(String questionId) {
        return http.get("/api/reviews/" + questionId + "/observations", Map.of(), new TypeReference<>() { });
    }

    public List<CriterionDto> criteria() {
        return http.get("/api/reviews/catalog/criteria", Map.of(), new TypeReference<>() { });
    }
}
