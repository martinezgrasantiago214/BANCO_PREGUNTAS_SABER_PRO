package co.unicauca.saberpro.question.application;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.events.DomainEvent;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.EventTopics;
import co.unicauca.saberpro.common.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.common.web.ConflictException;
import co.unicauca.saberpro.common.web.ForbiddenOperationException;
import co.unicauca.saberpro.common.web.ResourceNotFoundException;
import co.unicauca.saberpro.question.TestData;
import co.unicauca.saberpro.question.access.SQLiteQuestionRepository;
import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.factory.QuestionFactoryRegistry;
import co.unicauca.saberpro.question.domain.state.InvalidStateTransitionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU02 (PSP-8): el autor pasa su pregunta de Borrador a Pendiente de
 * revision. PSP-23: autor correcto, autor incorrecto, pregunta que no esta en
 * Borrador y publicacion de question.submitted.
 */
class SubmitForReviewTest {

    @TempDir
    Path tempDir;

    private SQLiteQuestionRepository repository;
    private QuestionApplicationService service;
    private final List<DomainEvent> published = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repository = new SQLiteQuestionRepository("jdbc:sqlite:" + tempDir.resolve("hu02.db"));
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(published::add);
        service = new QuestionApplicationService(repository, new QuestionFactoryRegistry(), bus);
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    /** Crea la pregunta con el caso de uso de HU01, para partir del estado inicial real. */
    private Question nueva() {
        Question q = service.create(TestData.validRequest("MULTIPLE_CHOICE"));
        published.clear();
        return q;
    }

    private Question guardadaEn(QuestionStatus status) {
        Question q = Question.builder().type("MULTIPLE_CHOICE").fromRequest(TestData.validRequest("MULTIPLE_CHOICE"))
                .status(status).build();
        repository.save(q);
        return q;
    }

    @Test
    void unaPreguntaNuevaNaceEnBorrador() {
        Question q = nueva();

        assertEquals(QuestionStatus.BORRADOR, service.findById(q.getId()).getStatus());
        assertTrue(q.isEditable());
    }

    @Test
    void elAutorEnviaSuBorradorYQuedaPendienteDeRevision() {
        Question q = nueva();

        Question updated = service.submitForReview(q.getId(), "autor1");

        assertEquals(QuestionStatus.PENDIENTE_REVISION, updated.getStatus());
        Question stored = service.findById(q.getId());
        assertEquals(QuestionStatus.PENDIENTE_REVISION, stored.getStatus());
        assertFalse(stored.isEditable());
        assertEquals("#90CAF9", stored.getStatus().getColorHex());
    }

    @Test
    void publicaQuestionSubmitted() {
        Question q = nueva();

        service.submitForReview(q.getId(), "autor1");

        assertEquals(1, published.size());
        QuestionSubmittedForReviewEvent event = assertInstanceOf(QuestionSubmittedForReviewEvent.class,
                published.get(0));
        assertEquals(EventTopics.QUESTION_SUBMITTED_FOR_REVIEW, event.routingKey());
        assertEquals(q.getId(), event.questionId());
        assertEquals("autor1", event.authorLogin());
        assertEquals(q.getDirectQuestion(), event.directQuestion());
    }

    @Test
    void otroUsuarioNoPuedeEnviarlaYNoCambiaNada() {
        Question q = nueva();

        assertThrows(ForbiddenOperationException.class, () -> service.submitForReview(q.getId(), "autor2"));
        assertThrows(ForbiddenOperationException.class, () -> service.submitForReview(q.getId(), null));

        assertEquals(QuestionStatus.BORRADOR, service.findById(q.getId()).getStatus());
        assertTrue(published.isEmpty());
    }

    @Test
    void noSePuedeEnviarDosVeces() {
        Question q = nueva();
        service.submitForReview(q.getId(), "autor1");
        published.clear();

        assertThrows(InvalidStateTransitionException.class, () -> service.submitForReview(q.getId(), "autor1"));
        assertTrue(published.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = QuestionStatus.class, names = {"PENDIENTE_REVISION", "APROBADA", "RECHAZADA", "ELIMINADA"})
    void soloUnBorradorSePuedeEnviar(QuestionStatus status) {
        Question q = guardadaEn(status);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.submitForReview(q.getId(), "autor1"));

        assertTrue(ex.getMessage().contains(status.getLabel()));
        assertEquals(status, service.findById(q.getId()).getStatus());
        assertTrue(published.isEmpty());
    }

    @Test
    void unaPreguntaInexistenteResponde404() {
        assertThrows(ResourceNotFoundException.class, () -> service.submitForReview("no-existe", "autor1"));
    }

    @Test
    void despuesDeEnviarlaYaNoSePuedeEditar() {
        Question q = nueva();
        service.submitForReview(q.getId(), "autor1");

        assertThrows(ConflictException.class,
                () -> service.update(q.getId(), TestData.validRequest("MULTIPLE_CHOICE")));
    }
}
