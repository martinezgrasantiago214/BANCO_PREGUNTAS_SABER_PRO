package co.unicauca.saberpro.frontend.facade;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.frontend.client.QuestionApi;
import co.unicauca.saberpro.frontend.client.ReviewApi;
import co.unicauca.saberpro.frontend.client.UserApi;
import co.unicauca.saberpro.frontend.model.Models.AssignmentView;
import co.unicauca.saberpro.frontend.model.Models.EvaluationView;
import co.unicauca.saberpro.frontend.model.Models.PageView;
import co.unicauca.saberpro.frontend.model.Models.PendingQuestionView;
import co.unicauca.saberpro.frontend.model.Models.QuestionForm;
import co.unicauca.saberpro.frontend.model.Models.QuestionView;
import co.unicauca.saberpro.frontend.model.Models.UserView;
import co.unicauca.saberpro.frontend.observer.Subject;

import java.util.List;

/**
 * Patron Facade (GoF, estructural): unico punto de entrada de las pantallas
 * Swing al backend. Oculta que hay tres microservicios detras del gateway,
 * las URLs, el JSON y la sesion del usuario: una pantalla solo llama, por
 * ejemplo, facade.createQuestion(form).
 *
 * Ademas es el sujeto (patron Observer) de los cambios en preguntas: cuando
 * algo cambia, avisa a las pantallas suscritas para que se refresquen.
 *
 * Todas las operaciones del API ya estan aqui; las que el backend aun no
 * implementa lanzan ApiException con isPendingFeature() == true (HTTP 501).
 */
public class BancoPreguntasFacade {

    private final UserApi users;
    private final QuestionApi questions;
    private final ReviewApi reviews;
    private final Subject questionChanges = new Subject();
    private UserView currentUser;

    public BancoPreguntasFacade(UserApi users, QuestionApi questions, ReviewApi reviews) {
        this.users = users;
        this.questions = questions;
        this.reviews = reviews;
    }

    // ---- Sesion y usuarios ----

    public UserView login(String login, String password) {
        currentUser = users.login(login, password);
        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }

    public UserView getCurrentUser() {
        return currentUser;
    }

    public UserView registerUser(String login, String fullName, String email, Role role, String password) {
        return users.register(login, fullName, email, role, password);
    }

    public List<UserView> listReviewers() {
        return users.listReviewers();
    }

    // ---- Preguntas (autor) ----

    /** HU01: crea la pregunta a nombre del usuario autenticado. */
    public QuestionView createQuestion(QuestionForm form) {
        QuestionView created = questions.create(form.withAuthor(requireUser().login()));
        questionChanges.notificarObservers();
        return created;
    }

    /** HU03: edita una pregunta propia en Borrador. */
    public QuestionView updateQuestion(String id, QuestionForm form) {
        QuestionView updated = questions.update(id, form.withAuthor(requireUser().login()));
        questionChanges.notificarObservers();
        return updated;
    }

    public QuestionView findQuestion(String id) {
        return questions.findById(id);
    }

    /** HU03: preguntas del usuario autenticado, con filtros y paginacion. */
    public PageView<QuestionView> searchMyQuestions(QuestionStatus status, String topic, String keyword,
                                                    int page, int size) {
        return questions.search(requireUser().login(), status, topic, keyword, page, size);
    }

    /** HU02: Borrador -> Pendiente de revision. */
    public QuestionView submitForReview(String questionId) {
        QuestionView updated = questions.submitForReview(questionId, requireUser().login());
        questionChanges.notificarObservers();
        return updated;
    }

    // ---- Revision por pares (administrador y revisor) ----

    /** HU04: preguntas pendientes que el administrador puede asignar. */
    public List<PendingQuestionView> listPendingQuestions() {
        return reviews.pendingQuestions();
    }

    /** HU04: asigna entre 1 y 3 revisores. */
    public List<AssignmentView> assignReviewers(String questionId, List<String> reviewerLogins) {
        List<AssignmentView> result = reviews.assign(questionId, reviewerLogins, requireUser().login());
        questionChanges.notificarObservers();
        return result;
    }

    /** HU05: preguntas asignadas al revisor autenticado. */
    public List<AssignmentView> listMyAssignments() {
        return reviews.assignedTo(requireUser().login());
    }

    /** HU05: el revisor autenticado evalua una pregunta. */
    public EvaluationView evaluate(String questionId, EvaluationVerdict verdict, String observations) {
        EvaluationView result = reviews.evaluate(questionId, requireUser().login(), verdict, observations);
        questionChanges.notificarObservers();
        return result;
    }

    /** HU05: observaciones de una pregunta, para el autor. */
    public List<EvaluationView> listEvaluations(String questionId) {
        return reviews.evaluations(questionId);
    }

    /** Sujeto al que se suscriben las pantallas que deben refrescarse cuando cambian las preguntas. */
    public Subject questionChanges() {
        return questionChanges;
    }

    private UserView requireUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No hay un usuario autenticado.");
        }
        return currentUser;
    }
}
