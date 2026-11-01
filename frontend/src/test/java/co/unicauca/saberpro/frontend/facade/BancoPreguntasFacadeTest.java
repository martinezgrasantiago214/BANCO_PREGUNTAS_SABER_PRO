package co.unicauca.saberpro.frontend.facade;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Patron Facade: las pantallas solo hablan con la fachada; aqui se usa con APIs falsas. */
class BancoPreguntasFacadeTest {

    private static final UserView AUTOR = new UserView(1, "autor1", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS, "ACTIVO");

    private final FakeQuestionApi questions = new FakeQuestionApi();
    private final FakeReviewApi reviews = new FakeReviewApi();
    private final BancoPreguntasFacade facade = new BancoPreguntasFacade(new FakeUserApi(), questions, reviews);

    private QuestionForm form() {
        return new QuestionForm("MULTIPLE_CHOICE", "Contexto", "¿Pregunta directa?", List.of("a", "b", "c", "d"),
                "a", "Justificacion", "Bibliografia", "Competencia", "Tema", "Subtema", DifficultyLevel.BAJO, null);
    }

    @Test
    void elLoginGuardaLaSesionYElLogoutLaBorra() {
        assertEquals(AUTOR, facade.login("autor1", "Autor123!"));
        assertEquals("autor1", facade.getCurrentUser().login());

        facade.logout();
        assertNull(facade.getCurrentUser());
    }

    @Test
    void alCrearUnaPreguntaLaFachadaPoneComoAutorAlUsuarioAutenticado() {
        facade.login("autor1", "Autor123!");
        facade.createQuestion(form());

        assertEquals("autor1", questions.lastForm.authorLogin());
    }

    @Test
    void lasPantallasSuscritasSeEnteranDeLosCambiosEnPreguntas() {
        facade.login("autor1", "Autor123!");
        int[] avisos = {0};
        facade.questionChanges().agregarObserver(() -> avisos[0]++);

        facade.createQuestion(form());
        facade.submitForReview("q-1");

        assertEquals(2, avisos[0]);
    }

    @Test
    void lasOperacionesDelUsuarioUsanElLoginDeLaSesion() {
        facade.login("autor1", "Autor123!");

        facade.searchMyQuestions(QuestionStatus.BORRADOR, null, null, 1, 10);
        assertEquals("autor1", questions.lastAuthor);

        facade.evaluate("q-1", EvaluationVerdict.APROBADA, "Bien");
        assertEquals("autor1", reviews.lastReviewer);
    }

    @Test
    void sinSesionNoSePuedenHacerOperacionesDeUsuario() {
        assertThrows(IllegalStateException.class, () -> facade.createQuestion(form()));
        assertThrows(IllegalStateException.class, facade::listMyAssignments);
    }

    // ---- APIs falsas ----

    private static final class FakeUserApi implements UserApi {
        @Override
        public UserView login(String login, String password) {
            return AUTOR;
        }

        @Override
        public UserView register(String login, String fullName, String email, Role role, String password) {
            return new UserView(2, login, fullName, email, role, "ACTIVO");
        }

        @Override
        public List<UserView> listReviewers() {
            return List.of();
        }
    }

    private static final class FakeQuestionApi implements QuestionApi {
        QuestionForm lastForm;
        String lastAuthor;

        private QuestionView view(String id) {
            return new QuestionView(id, "MULTIPLE_CHOICE", null, null, List.of(), null, null, null, null, null,
                    null, DifficultyLevel.BAJO, QuestionStatus.BORRADOR, "Borrador", "#FFE082", true, "autor1", null);
        }

        @Override
        public QuestionView create(QuestionForm form) {
            lastForm = form;
            return view("q-1");
        }

        @Override
        public QuestionView update(String id, QuestionForm form) {
            lastForm = form;
            return view(id);
        }

        @Override
        public QuestionView findById(String id) {
            return view(id);
        }

        @Override
        public PageView<QuestionView> search(String author, QuestionStatus status, String topic, String keyword,
                                             int page, int size) {
            lastAuthor = author;
            return new PageView<>(List.of(), page, size, 0, 1);
        }

        @Override
        public QuestionView submitForReview(String id, String authorLogin) {
            lastAuthor = authorLogin;
            return view(id);
        }
    }

    private static final class FakeReviewApi implements ReviewApi {
        String lastReviewer;

        @Override
        public List<PendingQuestionView> pendingQuestions() {
            return List.of();
        }

        @Override
        public List<AssignmentView> assign(String questionId, List<String> reviewers, String assignedBy) {
            return List.of();
        }

        @Override
        public List<AssignmentView> assignedTo(String reviewerLogin) {
            lastReviewer = reviewerLogin;
            return List.of();
        }

        @Override
        public EvaluationView evaluate(String questionId, String reviewerLogin, EvaluationVerdict verdict,
                                       String observations) {
            lastReviewer = reviewerLogin;
            return new EvaluationView("e-1", questionId, reviewerLogin, verdict, observations, null);
        }

        @Override
        public List<EvaluationView> evaluations(String questionId) {
            return List.of();
        }
    }
}
