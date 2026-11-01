package co.unicauca.saberpro.frontend.model;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.contract.Role;

import java.util.List;

/** Modelos del cliente: espejo del JSON que devuelve el API Gateway. */
public final class Models {

    private Models() {
    }

    public record UserView(int userId, String login, String fullName, String email, Role role, String status) {
    }

    /** Datos que el autor captura en el formulario de pregunta (HU01). */
    public record QuestionForm(String type, String context, String directQuestion, List<String> distractors,
                               String correctAnswer, String justification, String bibliography,
                               String competence, String topic, String subtopic,
                               DifficultyLevel difficultyLevel, String authorLogin) {

        public QuestionForm withAuthor(String login) {
            return new QuestionForm(type, context, directQuestion, distractors, correctAnswer, justification,
                    bibliography, competence, topic, subtopic, difficultyLevel, login);
        }
    }

    public record QuestionView(String id, String type, String context, String directQuestion,
                               List<String> distractors, String correctAnswer, String justification,
                               String bibliography, String competence, String topic, String subtopic,
                               DifficultyLevel difficultyLevel, QuestionStatus status, String statusLabel,
                               String statusColor, boolean editable, String authorLogin, String createdAt) {
    }

    public record PageView<T>(List<T> items, int page, int pageSize, long totalItems, int totalPages) {
    }

    /** HU04: pregunta Pendiente de revision con los revisores que ya tiene asignados. */
    public record PendingQuestionView(String questionId, String authorLogin, String directQuestion,
                                      String submittedAt, List<String> assignedReviewers) {

        public PendingQuestionView {
            assignedReviewers = assignedReviewers == null ? List.of() : List.copyOf(assignedReviewers);
        }
    }

    public record AssignmentView(String questionId, String reviewerLogin, String assignedBy, String assignedAt) {
    }

    public record EvaluationView(String id, String questionId, String reviewerLogin, EvaluationVerdict verdict,
                                 String observations, String evaluatedAt) {
    }
}
