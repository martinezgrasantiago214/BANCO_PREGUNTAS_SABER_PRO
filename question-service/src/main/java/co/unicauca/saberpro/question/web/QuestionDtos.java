package co.unicauca.saberpro.question.web;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.question.access.PagedResult;
import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.QuestionRequest;
import co.unicauca.saberpro.question.domain.factory.MultipleChoiceQuestionFactory;
import co.unicauca.saberpro.question.domain.factory.QuestionFactory;

import java.util.List;

/** DTOs del API del question-service: el JSON no depende de la forma interna de las entidades. */
public final class QuestionDtos {

    private QuestionDtos() {
    }

    /** Cuerpo de POST/PUT /api/questions. Si no se indica "type", se asume MULTIPLE_CHOICE. */
    public record QuestionInput(String type, String context, String directQuestion, List<String> distractors,
                                String correctAnswer, String justification, String bibliography,
                                String competence, String topic, String subtopic,
                                DifficultyLevel difficultyLevel, String authorLogin) {

        public QuestionRequest toRequest() {
            return new QuestionRequest(type != null && !type.isBlank() ? type : MultipleChoiceQuestionFactory.TYPE,
                    context, directQuestion, distractors, correctAnswer, justification, bibliography,
                    competence, topic, subtopic, difficultyLevel, authorLogin);
        }
    }

    /** Cuerpo de PATCH /api/questions/{id}/submit-review. */
    public record SubmitReviewInput(String authorLogin) {
    }

    /** Pregunta tal como la ve el cliente; incluye el color del estado (HU02). */
    public record QuestionResponse(String id, String type, String context, String directQuestion,
                                   List<String> distractors, String correctAnswer, String justification,
                                   String bibliography, String competence, String topic, String subtopic,
                                   DifficultyLevel difficultyLevel, QuestionStatus status, String statusLabel,
                                   String statusColor, boolean editable, String authorLogin, String createdAt) {

        public static QuestionResponse from(Question q) {
            return new QuestionResponse(q.getId(), q.getType(), q.getContext(), q.getDirectQuestion(),
                    q.getDistractors(), q.getCorrectAnswer(), q.getJustification(), q.getBibliography(),
                    q.getCompetence(), q.getTopic(), q.getSubtopic(), q.getDifficultyLevel(), q.getStatus(),
                    q.getStatus().getLabel(), q.getStatus().getColorHex(), q.isEditable(), q.getAuthorLogin(),
                    q.getCreatedAt().toString());
        }
    }

    public record PageResponse<T>(List<T> items, int page, int pageSize, long totalItems, int totalPages) {

        public static PageResponse<QuestionResponse> from(PagedResult<Question> result) {
            return new PageResponse<>(result.items().stream().map(QuestionResponse::from).toList(),
                    result.page(), result.pageSize(), result.totalItems(), result.totalPages());
        }
    }

    public record QuestionTypeResponse(String type, String name, int validationFilters) {

        public static QuestionTypeResponse from(QuestionFactory factory) {
            return new QuestionTypeResponse(factory.getType(), factory.getName(),
                    factory.createValidationPipeline().size());
        }
    }
}
