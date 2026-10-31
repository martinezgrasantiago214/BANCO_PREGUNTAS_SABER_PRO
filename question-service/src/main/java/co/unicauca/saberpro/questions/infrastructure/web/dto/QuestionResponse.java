package co.unicauca.saberpro.questions.infrastructure.web.dto;

import co.unicauca.saberpro.questions.domain.model.Question;

import java.time.Instant;
import java.util.List;

/** Representacion JSON de una pregunta; incluye el color del estado (HU02). */
public record QuestionResponse(
        String id,
        String type,
        String authorLogin,
        String context,
        String statement,
        List<String> options,
        String correctAnswer,
        String justification,
        String bibliography,
        String competence,
        String topic,
        String subtopic,
        String difficulty,
        String state,
        String stateLabel,
        String stateColor,
        boolean editable,
        Instant createdAt,
        Instant updatedAt) {

    public static QuestionResponse from(Question q) {
        return new QuestionResponse(q.getId(), q.getType(), q.getAuthorLogin(), q.getContext(), q.getStatement(),
                q.getOptionTexts(), q.getCorrectAnswer(), q.getJustification(), q.getBibliography(),
                q.getCompetence(), q.getTopic(), q.getSubtopic(), q.getDifficulty().name(),
                q.getStateType().name(), q.getStateType().getLabel(), q.getStateType().getColorHex(),
                q.isEditable(), q.getCreatedAt(), q.getUpdatedAt());
    }
}
