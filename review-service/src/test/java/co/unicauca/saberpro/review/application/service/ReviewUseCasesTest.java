package co.unicauca.saberpro.review.application.service;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.review.application.port.in.AssignReviewersUseCase.AssignReviewersCommand;
import co.unicauca.saberpro.review.application.port.in.EvaluateQuestionUseCase.EvaluateQuestionCommand;
import co.unicauca.saberpro.review.application.port.in.ReviewQueries.AssignedReview;
import co.unicauca.saberpro.review.application.port.out.DirectoryUnavailableException;
import co.unicauca.saberpro.review.domain.event.QuestionEvaluated;
import co.unicauca.saberpro.review.domain.event.ReviewRoundReopened;
import co.unicauca.saberpro.review.domain.event.ReviewersAssigned;
import co.unicauca.saberpro.review.domain.exception.ConcurrentReviewUpdateException;
import co.unicauca.saberpro.review.domain.exception.ReviewNotFoundException;
import co.unicauca.saberpro.review.domain.exception.ReviewRuleViolationException;
import co.unicauca.saberpro.review.domain.exception.ReviewerNotAllowedException;
import co.unicauca.saberpro.review.domain.model.QuestionReview;
import co.unicauca.saberpro.review.domain.model.ReviewStatus;
import co.unicauca.saberpro.review.domain.model.ReviewerAssignment;
import co.unicauca.saberpro.review.testdoubles.InMemoryQuestionReviewRepository;
import co.unicauca.saberpro.review.testdoubles.RecordingEventPublisher;
import co.unicauca.saberpro.review.testdoubles.ReviewFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.ADMIN;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.AUTHOR;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.QUESTION;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.T0;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.submission;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Casos de uso del review-service con dobles de los puertos de salida
 * (repositorio en memoria, directorio fijo y publicador que graba). Prueban
 * la orquestacion: cargar, delegar en el agregado, guardar y publicar.
 */
class ReviewUseCasesTest {

    private final Clock clock = Clock.fixed(T0.plusSeconds(600), ZoneOffset.UTC);
    private InMemoryQuestionReviewRepository repository;
    private RecordingEventPublisher publisher;
    private RegisterSubmissionService register;
    private AssignReviewersService assign;
    private EvaluateQuestionService evaluate;
    private ReviewQueryService queries;

    @BeforeEach
    void setUp() {
        repository = new InMemoryQuestionReviewRepository();
        publisher = new RecordingEventPublisher();
        register = new RegisterSubmissionService(repository, publisher);
        assign = new AssignReviewersService(repository, ReviewFixtures.directory(), publisher, clock);
        evaluate = new EvaluateQuestionService(repository, publisher, clock);
        queries = new ReviewQueryService(repository);
    }

    private void submittedAndAssigned(String... reviewers) {
        register.register(submission(1));
        assign.assign(new AssignReviewersCommand(QUESTION, List.of(reviewers), ADMIN));
    }

    // ---- registro de envios (HU02 -> HU04) ----

    @Test
    void registrarUnEnvioNuevoAbreLaRevisionYEsIdempotente() {
        assertTrue(register.register(submission(1)));
        assertFalse(register.register(submission(1)), "evento repetido de RabbitMQ");

        assertEquals(1, repository.saves());
        assertEquals(ReviewStatus.PENDIENTE_ASIGNACION, repository.findById(QUESTION).orElseThrow().status());
        assertEquals(1, queries.pendingQuestions().size());
    }

    // ---- HU04 ----

    @Test
    void hu04AsignarGuardaYPublicaElEventoParaNotificarALosRevisores() {
        register.register(submission(1));

        List<ReviewerAssignment> created = assign.assign(
                new AssignReviewersCommand(QUESTION, List.of(" rev1", "rev2 "), ADMIN));

        assertEquals(2, created.size());
        assertEquals(clock.instant(), created.get(0).assignedAt());
        assertEquals(ReviewStatus.EN_REVISION, repository.findById(QUESTION).orElseThrow().status());
        ReviewersAssigned event = publisher.ofType(ReviewersAssigned.class).get(0);
        assertEquals(List.of("rev1", "rev2"), event.reviewerLogins());
    }

    @Test
    void hu04UnaSeleccionInvalidaFallaSinConsultarElDirectorio() {
        register.register(submission(1));
        AtomicInteger lookups = new AtomicInteger();
        AssignReviewersService counting = new AssignReviewersService(repository, login -> {
            lookups.incrementAndGet();
            return ReviewFixtures.candidate(login);
        }, publisher, clock);

        assertThrows(ReviewRuleViolationException.class, () -> counting.assign(
                new AssignReviewersCommand(QUESTION, List.of("rev1", "rev1"), ADMIN)));
        assertEquals(0, lookups.get());
    }

    @Test
    void hu04NoSePuedeAsignarUnaPreguntaQueNuncaSeEnvioARevision() {
        assertThrows(ReviewNotFoundException.class,
                () -> assign.assign(new AssignReviewersCommand("no-existe", List.of("rev1"), ADMIN)));
        assertThrows(ReviewRuleViolationException.class,
                () -> assign.assign(new AssignReviewersCommand(" ", List.of("rev1"), ADMIN)));
        assertThrows(ReviewRuleViolationException.class,
                () -> assign.assign(new AssignReviewersCommand(QUESTION, List.of("rev1"), null)));
    }

    @Test
    void hu04SiElDirectorioNoRespondeNoSeGuardaNada() {
        register.register(submission(1));
        AssignReviewersService broken = new AssignReviewersService(repository, login -> {
            throw new DirectoryUnavailableException("caido", null);
        }, publisher, clock);

        assertThrows(DirectoryUnavailableException.class,
                () -> broken.assign(new AssignReviewersCommand(QUESTION, List.of("rev1"), ADMIN)));
        assertEquals(ReviewStatus.PENDIENTE_ASIGNACION, repository.findById(QUESTION).orElseThrow().status());
        assertTrue(publisher.published.isEmpty());
    }

    @Test
    void bloqueoOptimistaDosOperacionesSobreLaMismaVersionNoSePisan() {
        register.register(submission(1));
        QuestionReview a = repository.findById(QUESTION).orElseThrow();
        QuestionReview b = repository.findById(QUESTION).orElseThrow();

        a.assignReviewers(ReviewFixtures.candidates("rev1", "rev2"), ADMIN, T0);
        repository.save(a);
        b.assignReviewers(ReviewFixtures.candidates("rev3", "rev4"), ADMIN, T0);

        assertThrows(ConcurrentReviewUpdateException.class, () -> repository.save(b));
        assertEquals(List.of("rev1", "rev2"),
                repository.findById(QUESTION).orElseThrow().currentRound().reviewerLogins());
    }

    // ---- HU05 ----

    @Test
    void hu05EvaluarGuardaElVeredictoYPublicaElEvento() {
        submittedAndAssigned("rev1", "rev2");

        evaluate.evaluate(new EvaluateQuestionCommand(QUESTION, "rev1", EvaluationVerdict.RECHAZADA, "Ajustar C"));

        assertEquals(ReviewStatus.RECHAZADA, repository.findById(QUESTION).orElseThrow().status());
        QuestionEvaluated event = publisher.ofType(QuestionEvaluated.class).get(0);
        assertEquals("Ajustar C", event.observations());
        assertEquals(AUTHOR, event.authorLogin());
        assertTrue(queries.pendingQuestions().isEmpty(), "ya no esta pendiente");
    }

    @Test
    void hu05ElRevisorVeSusAsignacionesYCualesPuedeEvaluar() {
        submittedAndAssigned("rev1");

        List<AssignedReview> before = queries.assignedTo("rev1");
        assertEquals(1, before.size());
        assertTrue(before.get(0).awaitingMyEvaluation());
        assertTrue(queries.assignedTo("rev2").isEmpty());

        evaluate.evaluate(new EvaluateQuestionCommand(QUESTION, "rev1", EvaluationVerdict.APROBADA, ""));
        assertFalse(queries.assignedTo("rev1").get(0).awaitingMyEvaluation());
    }

    @Test
    void hu05EvaluarUnaPreguntaInexistenteResponde404() {
        assertThrows(ReviewNotFoundException.class, () -> evaluate.evaluate(
                new EvaluateQuestionCommand("x", "rev1", EvaluationVerdict.APROBADA, "")));
    }

    // ---- HU06 ----

    @Test
    void hu06FlujoCompletoRechazoVersionMejoradaYAprobacion() {
        submittedAndAssigned("rev1");
        evaluate.evaluate(new EvaluateQuestionCommand(QUESTION, "rev1", EvaluationVerdict.RECHAZADA,
                "La justificacion no cita la bibliografia."));

        assertTrue(register.register(submission(2)));
        ReviewRoundReopened reopened = publisher.ofType(ReviewRoundReopened.class).get(0);
        assertEquals(List.of("rev1"), reopened.previousReviewers());
        assertEquals(1, queries.pendingQuestions().size(), "la version 2 vuelve a la lista del administrador");

        assign.assign(new AssignReviewersCommand(QUESTION, List.of("rev2"), ADMIN));
        evaluate.evaluate(new EvaluateQuestionCommand(QUESTION, "rev2", EvaluationVerdict.APROBADA, "Corregida."));

        QuestionReview history = queries.history(QUESTION, AUTHOR, false);
        assertEquals(2, history.rounds().size());
        assertEquals(ReviewStatus.RECHAZADA, history.rounds().get(0).status());
        assertEquals(ReviewStatus.APROBADA, history.rounds().get(1).status());
        assertEquals(2, queries.evaluations(QUESTION, "rev1", false).size());

        List<AssignedReview> rev1 = queries.assignedTo("rev1");
        assertEquals(1, rev1.size());
        assertFalse(rev1.get(0).currentRound(), "su asignacion es de la ronda 1");
    }

    @Test
    void hu06ElHistorialNoLoVeCualquiera() {
        submittedAndAssigned("rev1");

        assertThrows(ReviewerNotAllowedException.class, () -> queries.history(QUESTION, "rev3", false));
        assertThrows(ReviewNotFoundException.class, () -> queries.history("otra", AUTHOR, false));
        assertNotNull(queries.history(QUESTION, ADMIN, true));
    }
}
