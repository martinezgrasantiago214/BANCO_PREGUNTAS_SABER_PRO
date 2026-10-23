package co.unicauca.saberpro.frontend.client;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.frontend.model.Models.AssignmentView;
import co.unicauca.saberpro.frontend.model.Models.EvaluationView;
import co.unicauca.saberpro.frontend.model.Models.PageView;
import co.unicauca.saberpro.frontend.model.Models.PendingQuestionView;
import co.unicauca.saberpro.frontend.model.Models.QuestionForm;
import co.unicauca.saberpro.frontend.model.Models.QuestionView;
import co.unicauca.saberpro.frontend.model.Models.UserView;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/** Implementaciones REST de las APIs del backend, todas a traves del API Gateway. */
public final class RestApis {

    private RestApis() {
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static final class RestUserApi implements UserApi {
        private final ApiClient client;

        public RestUserApi(ApiClient client) {
            this.client = client;
        }

        @Override
        public UserView login(String login, String password) {
            return client.post("/api/auth/login", Map.of("login", login, "password", password),
                    new TypeReference<>() { });
        }

        @Override
        public UserView register(String login, String fullName, String email, Role role, String password) {
            return client.post("/api/users", Map.of("login", login, "fullName", fullName, "email", email,
                    "role", role.name(), "password", password), new TypeReference<>() { });
        }

        @Override
        public List<UserView> listReviewers() {
            return client.get("/api/users/reviewers", new TypeReference<>() { });
        }
    }

    public static final class RestQuestionApi implements QuestionApi {
        private final ApiClient client;

        public RestQuestionApi(ApiClient client) {
            this.client = client;
        }

        @Override
        public QuestionView create(QuestionForm form) {
            return client.post("/api/questions", form, new TypeReference<>() { });
        }

        @Override
        public QuestionView update(String id, QuestionForm form) {
            return client.put("/api/questions/" + encode(id), form, new TypeReference<>() { });
        }

        @Override
        public QuestionView findById(String id) {
            return client.get("/api/questions/" + encode(id), new TypeReference<>() { });
        }

        @Override
        public PageView<QuestionView> search(String author, QuestionStatus status, String topic, String keyword,
                                             int page, int size) {
            StringJoiner query = new StringJoiner("&", "?", "");
            if (author != null) {
                query.add("author=" + encode(author));
            }
            if (status != null) {
                query.add("status=" + status.name());
            }
            if (topic != null && !topic.isBlank()) {
                query.add("topic=" + encode(topic));
            }
            if (keyword != null && !keyword.isBlank()) {
                query.add("keyword=" + encode(keyword));
            }
            query.add("page=" + page).add("size=" + size);
            return client.get("/api/questions" + query, new TypeReference<>() { });
        }

        @Override
        public QuestionView submitForReview(String id, String authorLogin) {
            return client.patch("/api/questions/" + encode(id) + "/submit-review",
                    Map.of("authorLogin", authorLogin), new TypeReference<>() { });
        }
    }

    public static final class RestReviewApi implements ReviewApi {
        private final ApiClient client;

        public RestReviewApi(ApiClient client) {
            this.client = client;
        }

        @Override
        public List<PendingQuestionView> pendingQuestions() {
            return client.get("/api/reviews/pending-questions", new TypeReference<>() { });
        }

        @Override
        public List<AssignmentView> assign(String questionId, List<String> reviewers, String assignedBy) {
            return client.post("/api/reviews/assignments", Map.of("questionId", questionId,
                    "reviewers", reviewers, "assignedBy", assignedBy), new TypeReference<>() { });
        }

        @Override
        public List<AssignmentView> assignedTo(String reviewerLogin) {
            return client.get("/api/reviews/assignments?reviewer=" + encode(reviewerLogin), new TypeReference<>() { });
        }

        @Override
        public EvaluationView evaluate(String questionId, String reviewerLogin, EvaluationVerdict verdict,
                                       String observations) {
            return client.post("/api/reviews/evaluations", Map.of("questionId", questionId,
                    "reviewerLogin", reviewerLogin, "verdict", verdict.name(),
                    "observations", observations == null ? "" : observations), new TypeReference<>() { });
        }

        @Override
        public List<EvaluationView> evaluations(String questionId) {
            return client.get("/api/reviews/evaluations?questionId=" + encode(questionId), new TypeReference<>() { });
        }
    }
}
