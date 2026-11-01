package co.unicauca.saberpro.review.application;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.events.DomainEvent;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.common.web.PendingIssueException;
import co.unicauca.saberpro.review.access.InMemoryReviewRepository;
import co.unicauca.saberpro.review.domain.Evaluation;
import co.unicauca.saberpro.review.domain.PendingQuestion;
import co.unicauca.saberpro.review.domain.ReviewAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Casos de uso del review-service.
 *
 * HU05 (PSP-50): listado de preguntas asignadas, evaluacion y publicacion del
 * evento. Las preguntas pendientes y las asignaciones se cargan directamente
 * en el repositorio, porque registrarlas es parte de HU04.
 */
class ReviewApplicationServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-30T15:00:00Z");

    private InMemoryReviewRepository repository;
    private final List<DomainEvent> published = new ArrayList<>();
    private ReviewApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewRepository();
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(published::add);
        service = new ReviewApplicationService(repository, bus);
    }

    /** Pregunta Pendiente de revision asignada a los revisores indicados. */
    private void pendingQuestionAssignedTo(String questionId, String... reviewers) {
        repository.savePendingQuestion(new PendingQuestion(questionId, "autor1", "¿Que patron usar?", T0));
        for (int i = 0; i < reviewers.length; i++) {
            repository.saveAssignment(new ReviewAssignment(questionId, reviewers[i], "admin", T0.plusSeconds(i)));
        }
    }

    // ---- PSP-45: preguntas asignadas ----

    @Test
    void elRevisorSoloVeSusAsignacionesDeLaMasRecienteALaMasAntigua() {
        repository.saveAssignment(new ReviewAssignment("q-1", "rev1", "admin", T0));
        repository.saveAssignment(new ReviewAssignment("q-2", "rev2", "admin", T0.plusSeconds(10)));
        repository.saveAssignment(new ReviewAssignment("q-3", "rev1", "admin", T0.plusSeconds(20)));

        List<ReviewAssignment> assigned = service.listAssignedTo("rev1");

        assertEquals(List.of("q-3", "q-1"), assigned.stream().map(ReviewAssignment::getQuestionId).toList());
        assertTrue(service.listAssignedTo("rev9").isEmpty());
    }

    @Test
    void sinLoginDeRevisorNoSeListan() {
        assertThrows(BusinessException.class, () -> service.listAssignedTo(" "));
        assertThrows(BusinessException.class, () -> service.listAssignedTo(null));
    }

    // ---- PSP-47 y PSP-49: evaluacion y evento ----

    @Test
    void unaEvaluacionValidaSeGuardaYPublicaElEventoConElAutorYLaPregunta() {
        pendingQuestionAssignedTo("q-1", "rev1");

        Evaluation evaluation = service.evaluate("q-1", "rev1", EvaluationVerdict.RECHAZADA,
                "  El distractor 'Capas' es demasiado obvio.  ");

        assertEquals("El distractor 'Capas' es demasiado obvio.", evaluation.getObservations());
        assertEquals(List.of(evaluation.getId()),
                service.listEvaluations("q-1").stream().map(Evaluation::getId).toList());

        assertEquals(1, published.size());
        QuestionEvaluatedEvent event = assertInstanceOf(QuestionEvaluatedEvent.class, published.get(0));
        assertEquals("q-1", event.questionId());
        assertEquals("autor1", event.authorLogin());
        assertEquals("¿Que patron usar?", event.directQuestion());
        assertEquals("rev1", event.reviewerLogin());
        assertEquals(EvaluationVerdict.RECHAZADA, event.verdict());
        assertEquals("El distractor 'Capas' es demasiado obvio.", event.observations());
    }

    @Test
    void sePuedeAprobarSinObservaciones() {
        pendingQuestionAssignedTo("q-1", "rev1");

        Evaluation evaluation = service.evaluate("q-1", "rev1", EvaluationVerdict.APROBADA, null);

        assertEquals("", evaluation.getObservations());
        assertEquals(1, published.size());
    }

    @Test
    void noSePuedeEvaluarUnaPreguntaQueNoEstaAsignadaAlRevisor() {
        pendingQuestionAssignedTo("q-1", "rev1");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.evaluate("q-1", "rev2", EvaluationVerdict.APROBADA, "Bien"));

        assertTrue(ex.getMessage().contains("no esta asignada al revisor rev2"));
        assertNothingSaved("q-1");
    }

    @Test
    void noSePuedeEvaluarUnaPreguntaQueNoEstaPendienteDeRevision() {
        repository.saveAssignment(new ReviewAssignment("q-1", "rev1", "admin", T0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.evaluate("q-1", "rev1", EvaluationVerdict.APROBADA, "Bien"));

        assertTrue(ex.getMessage().contains("no esta Pendiente de revision"));
        assertNothingSaved("q-1");
    }

    @Test
    void elPrimerVeredictoDecideYLaPreguntaNoAdmiteMasEvaluaciones() {
        pendingQuestionAssignedTo("q-1", "rev1", "rev2");
        service.evaluate("q-1", "rev1", EvaluationVerdict.APROBADA, "Bien");

        assertThrows(BusinessException.class,
                () -> service.evaluate("q-1", "rev1", EvaluationVerdict.RECHAZADA, "Lo pense mejor"));
        BusinessException other = assertThrows(BusinessException.class,
                () -> service.evaluate("q-1", "rev2", EvaluationVerdict.RECHAZADA, "No estoy de acuerdo"));

        assertTrue(other.getMessage().contains("ya fue evaluada por rev1 (Aprobada)"));
        assertEquals(1, service.listEvaluations("q-1").size());
        assertEquals(1, published.size());
    }

    @Test
    void lasReglasDeLaEvaluacionSeValidanAntesDeGuardar() {
        pendingQuestionAssignedTo("q-1", "rev1");

        assertThrows(BusinessException.class,
                () -> service.evaluate("q-1", "rev1", EvaluationVerdict.RECHAZADA, "   "));
        assertThrows(BusinessException.class, () -> service.evaluate("q-1", "rev1", null, "Bien"));
        assertThrows(BusinessException.class,
                () -> service.evaluate(" ", "rev1", EvaluationVerdict.APROBADA, "Bien"));
        assertThrows(BusinessException.class,
                () -> service.evaluate("q-1", null, EvaluationVerdict.APROBADA, "Bien"));
        assertNothingSaved("q-1");
    }

    @Test
    void lasObservacionesSeListanEnOrdenCronologico() {
        repository.saveEvaluation(new Evaluation("e-2", "q-1", "rev2", EvaluationVerdict.RECHAZADA, "Segunda",
                T0.plusSeconds(60)));
        repository.saveEvaluation(new Evaluation("e-1", "q-1", "rev1", EvaluationVerdict.APROBADA, "Primera", T0));

        assertEquals(List.of("e-1", "e-2"),
                service.listEvaluations("q-1").stream().map(Evaluation::getId).toList());
        assertTrue(service.listEvaluations("q-9").isEmpty());
        assertThrows(BusinessException.class, () -> service.listEvaluations(""));
    }

    // ---- HU04, pendiente ----

    @Test
    void losCasosDeUsoDeHu04PendientesIndicanSuIssue() {
        assertPending("PSP-30", () -> service.registerPendingQuestion(
                new QuestionSubmittedForReviewEvent("q-1", "autor1", "¿P?", Instant.now())));
        assertPending("PSP-30", service::listPendingQuestions);
        assertPending("PSP-32", () -> service.assignReviewers("q-1", List.of("rev1"), "admin"));
    }

    private void assertNothingSaved(String questionId) {
        assertTrue(service.listEvaluations(questionId).isEmpty());
        assertTrue(published.isEmpty());
    }

    private void assertPending(String issue, Runnable call) {
        PendingIssueException ex = assertThrows(PendingIssueException.class, call::run);
        assertEquals(issue, ex.getIssueKey());
    }
}
