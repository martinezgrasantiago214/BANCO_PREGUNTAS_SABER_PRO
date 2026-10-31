package co.unicauca.saberpro.client.api.dto;

import java.util.List;

public record EvaluationRequestDto(String verdict, List<Score> scores, String observation) {

    public record Score(String criterion, int score, String comment) {
    }
}
