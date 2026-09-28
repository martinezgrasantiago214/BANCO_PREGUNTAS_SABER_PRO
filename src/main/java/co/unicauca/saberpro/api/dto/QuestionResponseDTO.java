package co.unicauca.saberpro.api.dto;

import co.unicauca.saberpro.domain.Question;

import java.util.List;

/** Representacion JSON de salida de una pregunta. */
public record QuestionResponseDTO(
        String id,
        String type,
        String context,
        String directQuestion,
        List<String> distractors,
        String correctAnswer,
        String justification,
        String bibliography,
        String competence,
        String topic,
        String subtopic,
        String difficultyLevel,
        String state,
        String stateLabel,
        String authorLogin,
        String createdAt,
        List<String> assignedReviewers) {

    public static QuestionResponseDTO from(Question q) {
        return new QuestionResponseDTO(
                q.getId(),
                q.getType(),
                q.getContext(),
                q.getDirectQuestion(),
                q.getDistractors(),
                q.getCorrectAnswer(),
                q.getJustification(),
                q.getBibliography(),
                q.getCompetence(),
                q.getTopic(),
                q.getSubtopic(),
                q.getDifficultyLevel() != null ? q.getDifficultyLevel().name() : null,
                q.getState() != null ? q.getState().name() : null,
                q.getState() != null ? q.getState().getLabel() : null,
                q.getAuthorLogin(),
                q.getCreatedAt() != null ? q.getCreatedAt().toString() : null,
                q.getAssignedReviewers());
    }
}
