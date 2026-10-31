package co.unicauca.saberpro.client.api.dto;

import java.util.List;

public record QuestionRequestDto(String type, String context, String statement, List<String> options,
                                 String correctAnswer, String justification, String bibliography,
                                 String competence, String topic, String subtopic, String difficulty) {
}
