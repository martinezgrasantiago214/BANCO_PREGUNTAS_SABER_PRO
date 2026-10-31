package co.unicauca.saberpro.questions.application;

import co.unicauca.saberpro.questions.domain.exception.QuestionDomainException;
import co.unicauca.saberpro.questions.domain.exception.QuestionDomainException.Kind;
import co.unicauca.saberpro.questions.domain.lifecycle.QuestionStateType;
import co.unicauca.saberpro.questions.domain.model.Question;
import co.unicauca.saberpro.questions.domain.plugin.MultipleChoiceQuestionPlugin;
import co.unicauca.saberpro.questions.domain.plugin.QuestionTypeRegistry;
import co.unicauca.saberpro.questions.testdoubles.InMemoryQuestionRepository;
import co.unicauca.saberpro.questions.testdoubles.QuestionFixtures;
import co.unicauca.saberpro.questions.testdoubles.RecordingEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionServiceTest {

    private InMemoryQuestionRepository repository;
    private RecordingEventPublisher publisher;
    private QuestionService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryQuestionRepository();
        publisher = new RecordingEventPublisher();
        service = new QuestionService(repository, publisher,
                new QuestionTypeRegistry(List.of(new MultipleChoiceQuestionPlugin())));
    }

    @Test
    void hu01CreatesDraftQuestion() {
        Question q = service.create(QuestionFixtures.valid("autor1"));
        assertEquals(QuestionStateType.BORRADOR, q.getStateType());
        assertTrue(repository.findById(q.getId()).isPresent());
        assertTrue(publisher.submitted.isEmpty(), "crear no publica eventos");
    }

    @Test
    void hu01InvalidQuestionIsNotSaved() {
        assertThrows(QuestionDomainException.class,
                () -> service.create(QuestionFixtures.withStatement(QuestionFixtures.valid("autor1"), "")));
        assertEquals(0, repository.saves);
    }

    @Test
    void hu02SubmitForReviewChangesStateAndPublishesEvent() {
        Question q = service.create(QuestionFixtures.valid("autor1"));
        Question submitted = service.submitForReview(q.getId(), "autor1");
        assertEquals(QuestionStateType.PENDIENTE_REVISION, submitted.getStateType());
        assertEquals(List.of(q.getId()), publisher.submitted);
    }

    @Test
    void hu02OnlyAuthorCanSubmitAndOnlyOnce() {
        Question q = service.create(QuestionFixtures.valid("autor1"));
        assertEquals(Kind.FORBIDDEN,
                assertThrows(QuestionDomainException.class, () -> service.submitForReview(q.getId(), "autor2")).getKind());
        service.submitForReview(q.getId(), "autor1");
        assertEquals(Kind.INVALID_TRANSITION,
                assertThrows(QuestionDomainException.class, () -> service.submitForReview(q.getId(), "autor1")).getKind());
        assertEquals(1, publisher.submitted.size());
    }

    @Test
    void hu03ListsOnlyAuthorQuestionsWithPagination() {
        for (int i = 0; i < 7; i++) {
            service.create(QuestionFixtures.valid("autor1"));
        }
        service.create(QuestionFixtures.valid("autor2"));
        PageResult<Question> page2 = service.search(
                new QuestionSearchCriteria("autor1", null, null, null, null, null, 2, 5));
        assertEquals(2, page2.items().size());
        assertEquals(7, page2.totalItems());
        assertEquals(2, page2.totalPages());
    }

    @Test
    void hu03UpdateRequiresOwnerAndEditableState() {
        Question q = service.create(QuestionFixtures.valid("autor1"));
        var changed = QuestionFixtures.withStatement(QuestionFixtures.valid("autor1"), "¿Cuál es el nuevo enunciado editado?");
        assertEquals(Kind.FORBIDDEN, assertThrows(QuestionDomainException.class,
                () -> service.update(q.getId(), changed, "autor2")).getKind());
        assertEquals("¿Cuál es el nuevo enunciado editado?", service.update(q.getId(), changed, "autor1").getStatement());
        service.submitForReview(q.getId(), "autor1");
        assertEquals(Kind.INVALID_TRANSITION, assertThrows(QuestionDomainException.class,
                () -> service.update(q.getId(), changed, "autor1")).getKind());
    }

    @Test
    void reviewEventsDriveTheLifecycleIdempotently() {
        Question q = service.create(QuestionFixtures.valid("autor1"));
        service.submitForReview(q.getId(), "autor1");
        service.markInReview(q.getId());
        service.markInReview(q.getId()); // evento repetido
        assertEquals(QuestionStateType.EN_REVISION, service.findById(q.getId()).getStateType());
        service.applyReviewDecision(q.getId(), true);
        service.applyReviewDecision(q.getId(), true); // evento repetido
        assertEquals(QuestionStateType.APROBADA, service.findById(q.getId()).getStateType());
    }

    @Test
    void decisionArrivingBeforeAssignmentIsTolerated() {
        Question q = service.create(QuestionFixtures.valid("autor1"));
        service.submitForReview(q.getId(), "autor1");
        service.applyReviewDecision(q.getId(), false);
        assertEquals(QuestionStateType.RECHAZADA, service.findById(q.getId()).getStateType());
    }

    @Test
    void deleteOnlyDraftsOwnedByRequester() {
        Question q = service.create(QuestionFixtures.valid("autor1"));
        service.delete(q.getId(), "autor1");
        assertEquals(Kind.NOT_FOUND,
                assertThrows(QuestionDomainException.class, () -> service.findById(q.getId())).getKind());
    }
}
