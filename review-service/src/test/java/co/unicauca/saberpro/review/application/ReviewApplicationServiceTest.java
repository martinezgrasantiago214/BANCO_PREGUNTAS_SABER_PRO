package co.unicauca.saberpro.review.application;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.common.events.DomainEvent;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.common.events.ReviewersAssignedEvent;
import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.review.access.InMemoryReviewRepository;
import co.unicauca.saberpro.review.directory.UserRoleDirectory;
import co.unicauca.saberpro.review.domain.Evaluation;
import co.unicauca.saberpro.review.domain.PendingQuestion;
import co.unicauca.saberpro.review.domain.ReviewAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Casos de uso del review-service.
 *
 * HU04 (PSP-35): registro de preguntas pendientes desde el evento y
 * asignacion valida e invalida de revisores. El user-service se reemplaza
 * por un directorio de roles en memoria.
 *
 * HU05 (PSP-50): listado de preguntas asignadas, evaluacion y publicacion del
 * evento. En esas pruebas las preguntas pendientes y las asignaciones se
 * cargan directamente en el repositorio.
 */
class ReviewApplicationServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-30T15:00:00Z");

    private InMemoryReviewRepository repository;
    private final List<DomainEvent> published = new ArrayList<>();
    private ReviewApplicationService service;

    /** Usuarios de prueba del user-service y su rol. */
    private final Map<String, Role> roles = new HashMap<>(Map.of(
            "rev1", Role.REVISOR, "rev2", Role.REVISOR, "rev3", Role.REVISOR, "rev4", Role.REVISOR,
            "autor1", Role.AUTOR_PREGUNTAS, "admin", Role.ADMINISTRADOR));

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewRepository();
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(published::add);
        UserRoleDirectory directory = login -> Optional.ofNullable(roles.get(login));
        service = new ReviewApplicationService(repository, bus, directory);
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

    // ---- HU04 (PSP-30): preguntas pendientes ----

    private void submitted(String questionId, Instant at) {
        service.registerPendingQuestion(new QuestionSubmittedForReviewEvent(questionId, "autor1",
                "¿Que patron usar en " + questionId + "?", at));
    }

    @Test
    void unaPreguntaEnviadaARevisionApareceEnElListadoDelAdministrador() {
        submitted("q-2", T0.plusSeconds(60));
        submitted("q-1", T0);

        List<PendingQuestion> pending = service.listPendingQuestions();

        assertEquals(List.of("q-1", "q-2"), pending.stream().map(PendingQuestion::questionId).toList());
        assertEquals("autor1", pending.get(0).authorLogin());
        assertEquals("¿Que patron usar en q-1?", pending.get(0).directQuestion());
        assertEquals(T0, pending.get(0).submittedAt());
    }

    @Test
    void siElEventoLlegaDosVecesLaPreguntaSeListaUnaSolaVez() {
        submitted("q-1", T0);
        submitted("q-1", T0);

        assertEquals(1, service.listPendingQuestions().size());
    }

    @Test
    void unEventoSinPreguntaSeRechaza() {
        assertThrows(BusinessException.class, () -> service.registerPendingQuestion(
                new QuestionSubmittedForReviewEvent(" ", "autor1", "¿P?", T0)));
        assertTrue(service.listPendingQuestions().isEmpty());
    }

    @Test
    void unaPreguntaYaEvaluadaNoSeListaComoPendiente() {
        pendingQuestionAssignedTo("q-1", "rev1");
        submitted("q-2", T0);
        service.evaluate("q-1", "rev1", EvaluationVerdict.APROBADA, "Bien");

        assertEquals(List.of("q-2"), service.listPendingQuestions().stream().map(PendingQuestion::questionId).toList());
    }

    // ---- HU04 (PSP-31 y PSP-32): asignacion valida ----

    @Test
    void laAsignacionValidaSeGuardaYPublicaElEventoConLosRevisores() {
        submitted("q-1", T0);

        List<ReviewAssignment> created = service.assignReviewers("q-1", List.of("rev1", " rev2 "), "admin");

        assertEquals(List.of("rev1", "rev2"), created.stream().map(ReviewAssignment::getReviewerLogin).toList());
        assertTrue(created.stream().allMatch(a -> "admin".equals(a.getAssignedBy())));
        assertEquals(List.of("rev1", "rev2"),
                service.listAssignmentsOf("q-1").stream().map(ReviewAssignment::getReviewerLogin).toList());
        assertEquals(List.of("q-1"), service.listAssignedTo("rev1").stream().map(ReviewAssignment::getQuestionId).toList());

        assertEquals(1, published.size());
        ReviewersAssignedEvent event = assertInstanceOf(ReviewersAssignedEvent.class, published.get(0));
        assertEquals("q-1", event.questionId());
        assertEquals("¿Que patron usar en q-1?", event.directQuestion());
        assertEquals(List.of("rev1", "rev2"), event.reviewerLogins());
        assertEquals("admin", event.assignedBy());
    }

    @Test
    void sePuedenAgregarRevisoresHastaCompletarTres() {
        submitted("q-1", T0);
        service.assignReviewers("q-1", List.of("rev1"), "admin");

        service.assignReviewers("q-1", List.of("rev2", "rev3"), "admin");

        assertEquals(3, service.listAssignmentsOf("q-1").size());
        assertEquals(2, published.size());
    }

    // ---- HU04 (PSP-31 y PSP-32): asignacion invalida ----

    @Test
    void hayQueElegirEntreUnoYTresRevisores() {
        submitted("q-1", T0);

        assertRejected("al menos 1 revisor", () -> service.assignReviewers("q-1", List.of(), "admin"));
        assertRejected("al menos 1 revisor", () -> service.assignReviewers("q-1", null, "admin"));
        assertRejected("mas de 3 revisores",
                () -> service.assignReviewers("q-1", List.of("rev1", "rev2", "rev3", "rev4"), "admin"));
        assertRejected("mismo revisor dos veces",
                () -> service.assignReviewers("q-1", List.of("rev1", "rev1"), "admin"));
        assertNothingAssigned("q-1");
    }

    @Test
    void conLosYaAsignadosNoSePuedenSuperarTresRevisores() {
        submitted("q-1", T0);
        service.assignReviewers("q-1", List.of("rev1", "rev2"), "admin");
        published.clear();

        assertRejected("no puede tener mas de 3",
                () -> service.assignReviewers("q-1", List.of("rev3", "rev4"), "admin"));
        assertEquals(2, service.listAssignmentsOf("q-1").size());
        assertTrue(published.isEmpty());
    }

    @Test
    void elAutorNoPuedeSerRevisorDeSuPropiaPregunta() {
        roles.put("autor1", Role.REVISOR);
        submitted("q-1", T0);

        assertRejected("No puede asignar al autor (autor1)",
                () -> service.assignReviewers("q-1", List.of("autor1"), "admin"));
        assertNothingAssigned("q-1");
    }

    @Test
    void soloSePuedenElegirUsuariosConRolRevisor() {
        submitted("q-1", T0);

        assertRejected("admin no tiene el rol Revisor",
                () -> service.assignReviewers("q-1", List.of("rev1", "admin"), "admin"));
        assertRejected("fantasma no existe", () -> service.assignReviewers("q-1", List.of("fantasma"), "admin"));
        assertNothingAssigned("q-1");
    }

    @Test
    void noSePuedeAsignarDosVecesElMismoRevisorAUnaPregunta() {
        submitted("q-1", T0);
        service.assignReviewers("q-1", List.of("rev1"), "admin");
        published.clear();

        assertRejected("rev1 ya esta asignado", () -> service.assignReviewers("q-1", List.of("rev1"), "admin"));
        assertEquals(1, service.listAssignmentsOf("q-1").size());
        assertTrue(published.isEmpty());
    }

    @Test
    void soloSeAsignanRevisoresAPreguntasPendientesDeRevision() {
        assertRejected("no esta Pendiente de revision",
                () -> service.assignReviewers("q-9", List.of("rev1"), "admin"));

        pendingQuestionAssignedTo("q-1", "rev1");
        service.evaluate("q-1", "rev1", EvaluationVerdict.APROBADA, "Bien");
        published.clear();
        assertRejected("ya fue evaluada", () -> service.assignReviewers("q-1", List.of("rev2"), "admin"));
        assertTrue(published.isEmpty());
    }

    @Test
    void sinPreguntaOSinAdministradorNoSeAsigna() {
        submitted("q-1", T0);

        assertThrows(BusinessException.class, () -> service.assignReviewers(" ", List.of("rev1"), "admin"));
        assertThrows(BusinessException.class, () -> service.assignReviewers("q-1", List.of("rev1"), null));
        assertNothingAssigned("q-1");
    }

    private void assertRejected(String expectedMessage, Runnable call) {
        BusinessException ex = assertThrows(BusinessException.class, call::run);
        assertTrue(ex.getMessages().stream().anyMatch(m -> m.contains(expectedMessage)),
                () -> "Se esperaba un error con '" + expectedMessage + "', se obtuvo " + ex.getMessages());
    }

    private void assertNothingAssigned(String questionId) {
        assertTrue(service.listAssignmentsOf(questionId).isEmpty());
        assertTrue(published.isEmpty());
    }

    private void assertNothingSaved(String questionId) {
        assertTrue(service.listEvaluations(questionId).isEmpty());
        assertTrue(published.isEmpty());
    }
}
