package co.unicauca.saberpro.client.api.dto;

import java.time.Instant;
import java.util.List;

public record QuestionDto(String id, String type, String authorLogin, String context, String statement,
                          List<String> options, String correctAnswer, String justification, String bibliography,
                          String competence, String topic, String subtopic, String difficulty, String state,
                          String stateLabel, String stateColor, boolean editable, Instant createdAt,
                          Instant updatedAt) {
}
