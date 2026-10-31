package co.unicauca.saberpro.client.api;

import co.unicauca.saberpro.client.api.dto.*;
import co.unicauca.saberpro.client.api.http.JsonHttpClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Patron Facade (GoF, estructural). El frontend habla con CUATRO
 * microservicios (usuarios, preguntas, revisiones y notificaciones), cada
 * uno con sus rutas, DTOs y reglas de cabeceras. La fachada ofrece una
 * interfaz unica y de alto nivel, orientada a los casos de uso, y oculta:
 * - que clientes HTTP existen y a que servicio pertenece cada operacion,
 * - la sesion del usuario y la propagacion de su identidad (X-User-Login),
 * - la composicion de datos de varios servicios (p. ej. pregunta + revision).
 * Los controladores de la UI solo dependen de esta clase.
 */
public class SaberProApiFacade {

    private final UserApiClient users;
    private final QuestionApiClient questions;
    private final ReviewApiClient reviews;
    private final NotificationApiClient notifications;
    private UserDto currentUser;

    public SaberProApiFacade(String gatewayUrl) {
        JsonHttpClient http = new JsonHttpClient(gatewayUrl, () -> currentUser == null ? null : currentUser.login());
        this.users = new UserApiClient(http);
        this.questions = new QuestionApiClient(http);
        this.reviews = new ReviewApiClient(http);
        this.notifications = new NotificationApiClient(http);
    }

    // ------------------------------------------------------------ sesion
    public UserDto login(String login, String password) {
        currentUser = users.login(login, password);
        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }

    public Optional<UserDto> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public UserDto registerUser(String login, String fullName, String email, String role, String password) {
        return users.register(login, fullName, email, role, password);
    }

    // ------------------------------------------------------------ autor (HU01-HU03)
    public QuestionDto createQuestion(QuestionRequestDto request) {
        return questions.create(request);
    }

    public QuestionDto updateQuestion(String id, QuestionRequestDto request) {
        return questions.update(id, request);
    }

    public void deleteQuestion(String id) {
        questions.delete(id);
    }

    public QuestionDto submitForReview(String id) {
        return questions.submitForReview(id);
    }

    public PageDto<QuestionDto> myQuestions(String state, String topic, String keyword, int page, int size) {
        Map<String, Object> f = new HashMap<>();
        f.put("author", currentUser.login());
        f.put("state", state);
        f.put("topic", topic);
        f.put("keyword", keyword);
        f.put("page", page);
        f.put("size", size);
        return questions.search(f);
    }

    public QuestionDto question(String id) {
        return questions.get(id);
    }

    public List<CatalogItemDto> questionStates() {
        return questions.states();
    }

    /** Observaciones de los revisores; vacio si la pregunta aun no se ha enviado a revision. */
    public List<ObservationDto> observationsOf(String questionId) {
        try {
            return reviews.observations(questionId);
        } catch (co.unicauca.saberpro.client.api.http.ApiException ex) {
            if (ex.getStatus() == 404) {
                return List.of();
            }
            throw ex;
        }
    }

    // ------------------------------------------------------------ administrador (HU04)
    public PageDto<ReviewDto> pendingAssignment(int page, int size) {
        return reviews.pendingAssignment(page, size);
    }

    public List<UserDto> reviewers() {
        return users.byRole("REVISOR");
    }

    public ReviewDto assignReviewers(String questionId, List<String> reviewerLogins) {
        return reviews.assignReviewers(questionId, reviewerLogins);
    }

    // ------------------------------------------------------------ revisor (HU05)
    public PageDto<ReviewDto> assignedToMe(String assignmentStatus, int page, int size) {
        return reviews.assignedToMe(assignmentStatus, page, size);
    }

    /** Compone datos de dos servicios: la pregunta completa y su proceso de revision. */
    public ReviewWorkspace reviewWorkspace(String questionId) {
        return new ReviewWorkspace(questions.get(questionId), reviews.get(questionId), reviews.criteria());
    }

    public ReviewDto evaluate(String questionId, EvaluationRequestDto evaluation) {
        return reviews.evaluate(questionId, evaluation);
    }

    public ObservationDto addObservation(String questionId, String text) {
        return reviews.observe(questionId, text);
    }

    // ------------------------------------------------------------ notificaciones
    public NotificationDto.Page inbox(int page, int size) {
        return notifications.inbox(page, size);
    }

    public void markNotificationRead(long id) {
        notifications.markRead(id);
    }

    /** Vista compuesta para la pantalla de evaluacion del revisor. */
    public record ReviewWorkspace(QuestionDto question, ReviewDto review, List<CriterionDto> criteria) {
    }
}
