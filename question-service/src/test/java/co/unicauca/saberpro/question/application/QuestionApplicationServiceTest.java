package co.unicauca.saberpro.question.application;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.common.web.PendingIssueException;
import co.unicauca.saberpro.common.web.ResourceNotFoundException;
import co.unicauca.saberpro.question.TestData;
import co.unicauca.saberpro.question.access.QuestionFilterCriteria;
import co.unicauca.saberpro.question.access.SQLiteQuestionRepository;
import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.factory.QuestionFactoryRegistry;
import co.unicauca.saberpro.question.domain.state.InvalidStateTransitionException;
import co.unicauca.saberpro.common.events.DomainEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Casos de uso del question-service. Las pruebas de los casos de uso
 * pendientes verifican que respondan con su issue de Jira; al implementarlos,
 * reemplacelas por pruebas del comportamiento real (PSP-19, PSP-23, PSP-29, PSP-50).
 */
class QuestionApplicationServiceTest {

    @TempDir
    Path tempDir;

    private SQLiteQuestionRepository repository;
    private QuestionApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new SQLiteQuestionRepository("jdbc:sqlite:" + tempDir.resolve("test.db"));
        service = new QuestionApplicationService(repository, new QuestionFactoryRegistry(), new DomainEventBus());
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void consultaUnaPreguntaPorId() {
        Question q = Question.builder().type("MULTIPLE_CHOICE").fromRequest(TestData.validRequest("x")).build();
        repository.save(q);

        assertEquals(q.getId(), service.findById(q.getId()).getId());
        assertThrows(ResourceNotFoundException.class, () -> service.findById("no-existe"));
    }

    @Test
    void exponeLosTiposDePreguntaCargados() {
        assertEquals(3, service.availableTypes().size());
    }

    @Test
    void losCasosDeUsoPendientesIndicanSuIssue() {
        assertPending("PSP-18", () -> service.create(TestData.validRequest("MULTIPLE_CHOICE")));
        assertPending("PSP-24", () -> service.search(new QuestionFilterCriteria()));
        assertPending("PSP-28", () -> service.update("q-1", TestData.validRequest("MULTIPLE_CHOICE")));
        assertPending("PSP-21", () -> service.submitForReview("q-1", "autor1"));
    }

    // ---- HU05 (PSP-48 y PSP-50): veredicto del revisor ----

    private Question savedWithStatus(QuestionStatus status) {
        Question q = Question.builder().type("MULTIPLE_CHOICE").fromRequest(TestData.validRequest("x"))
                .status(status).build();
        repository.save(q);
        return q;
    }

    private static QuestionEvaluatedEvent evaluated(String questionId, EvaluationVerdict verdict) {
        return new QuestionEvaluatedEvent(questionId, "autor1", "¿P?", "rev1", verdict, "Observaciones",
                Instant.now());
    }

    @Test
    void unaPreguntaPendienteQuedaAprobadaYSeGuarda() {
        Question q = savedWithStatus(QuestionStatus.PENDIENTE_REVISION);

        Question result = service.applyEvaluation(evaluated(q.getId(), EvaluationVerdict.APROBADA));

        assertEquals(QuestionStatus.APROBADA, result.getStatus());
        assertEquals(QuestionStatus.APROBADA, repository.findById(q.getId()).orElseThrow().getStatus());
    }

    @Test
    void unaPreguntaPendienteQuedaRechazadaYSeGuarda() {
        Question q = savedWithStatus(QuestionStatus.PENDIENTE_REVISION);

        service.applyEvaluation(evaluated(q.getId(), EvaluationVerdict.RECHAZADA));

        assertEquals(QuestionStatus.RECHAZADA, repository.findById(q.getId()).orElseThrow().getStatus());
    }

    @Test
    void recibirElMismoEventoDosVecesNoFalla() {
        Question q = savedWithStatus(QuestionStatus.PENDIENTE_REVISION);
        service.applyEvaluation(evaluated(q.getId(), EvaluationVerdict.APROBADA));

        Question again = service.applyEvaluation(evaluated(q.getId(), EvaluationVerdict.APROBADA));

        assertEquals(QuestionStatus.APROBADA, again.getStatus());
    }

    @Test
    void elPatronStateImpideEvaluarUnaPreguntaQueNoEstaPendiente() {
        Question draft = savedWithStatus(QuestionStatus.BORRADOR);
        Question approved = savedWithStatus(QuestionStatus.APROBADA);

        assertThrows(InvalidStateTransitionException.class,
                () -> service.applyEvaluation(evaluated(draft.getId(), EvaluationVerdict.APROBADA)));
        assertThrows(InvalidStateTransitionException.class,
                () -> service.applyEvaluation(evaluated(approved.getId(), EvaluationVerdict.RECHAZADA)));
        assertEquals(QuestionStatus.BORRADOR, repository.findById(draft.getId()).orElseThrow().getStatus());
        assertEquals(QuestionStatus.APROBADA, repository.findById(approved.getId()).orElseThrow().getStatus());
    }

    @Test
    void unEventoDeUnaPreguntaInexistenteOIncompletoSeRechaza() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.applyEvaluation(evaluated("no-existe", EvaluationVerdict.APROBADA)));
        assertThrows(BusinessException.class, () -> service.applyEvaluation(evaluated("q-1", null)));
        assertThrows(BusinessException.class, () -> service.applyEvaluation(null));
    }

    private void assertPending(String issue, Runnable call) {
        PendingIssueException ex = assertThrows(PendingIssueException.class, call::run);
        assertEquals(issue, ex.getIssueKey());
    }
}
