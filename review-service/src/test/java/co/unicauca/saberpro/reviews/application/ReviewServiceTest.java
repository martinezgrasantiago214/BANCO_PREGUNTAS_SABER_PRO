package co.unicauca.saberpro.reviews.application;

import co.unicauca.saberpro.reviews.domain.exception.ReviewDomainException;
import co.unicauca.saberpro.reviews.domain.exception.ReviewDomainException.Kind;
import co.unicauca.saberpro.reviews.domain.model.AssignmentStatus;
import co.unicauca.saberpro.reviews.domain.model.ReviewStatus;
import co.unicauca.saberpro.reviews.domain.model.Verdict;
import co.unicauca.saberpro.reviews.domain.policy.UnanimousApprovalPolicy;
import co.unicauca.saberpro.reviews.testdoubles.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReviewServiceTest {

    private InMemoryReviewRepository repository;
    private RecordingReviewPublisher publisher;
    private ReviewService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewRepository();
        publisher = new RecordingReviewPublisher();
        service = new ReviewService(repository, publisher, new FakeUserDirectory(), new UnanimousApprovalPolicy());
        service.registerSubmission("q1", "autor1", "¿Pregunta uno?", "Diseño", "Arquitectura");
    }

    @Test
    void registerSubmissionIsIdempotent() {
        service.registerSubmission("q1", "autor1", "¿Pregunta uno?", "Diseño", "Arquitectura");
        assertEquals(1, service.listPendingAssignment(1, 10).totalItems());
        assertEquals(1, repository.saves);
    }

    @Test
    void hu04AdminAssignsReviewersAndEventIsPublished() {
        service.assignReviewers("q1", List.of("rev1", "rev2"), "admin");
        assertEquals(ReviewStatus.EN_REVISION, service.get("q1").getStatus());
        assertEquals(List.of("ASSIGNED:rev1,rev2"), publisher.events);
        assertEquals(0, service.listPendingAssignment(1, 10).totalItems());
    }

    @Test
    void hu04OnlyAdminsCanAssign() {
        assertEquals(Kind.FORBIDDEN, assertThrows(ReviewDomainException.class,
                () -> service.assignReviewers("q1", List.of("rev1"), "autor1")).getKind());
        assertTrue(publisher.events.isEmpty());
    }

    @Test
    void hu04AssignedUsersMustBeReviewers() {
        ReviewDomainException ex = assertThrows(ReviewDomainException.class,
                () -> service.assignReviewers("q1", List.of("rev1", "fantasma"), "admin"));
        assertEquals(List.of("fantasma"), ex.getDetails());
        assertEquals(ReviewStatus.PENDIENTE_ASIGNACION, repository.findByQuestionId("q1").orElseThrow().getStatus(),
                "no se persiste una asignacion invalida");
    }

    @Test
    void hu05ReviewerListsAssignedQuestions() {
        service.registerSubmission("q2", "autor1", "¿Pregunta dos?", "Diseño", "Patrones");
        service.assignReviewers("q1", List.of("rev1"), "admin");
        service.assignReviewers("q2", List.of("rev1", "rev2"), "admin");
        assertEquals(2, service.listAssignedTo("rev1", AssignmentStatus.ASIGNADA, 1, 10).totalItems());
        assertEquals(1, service.listAssignedTo("rev2", null, 1, 10).totalItems());
        assertEquals(0, service.listAssignedTo("rev3", null, 1, 10).totalItems());
    }

    @Test
    void hu05EvaluationPublishesEvaluationObservationAndDecision() {
        service.assignReviewers("q1", List.of("rev1"), "admin");
        publisher.events.clear();
        service.submitEvaluation("q1", "rev1",
                new EvaluationCommand(Verdict.APROBADA, ReviewFixtures.allScores(5), "Excelente pregunta"));
        assertEquals(List.of("EVALUATED:rev1:APROBADA", "OBSERVATION:rev1", "COMPLETED:APROBADA"), publisher.events);
        assertEquals(ReviewStatus.APROBADA, service.get("q1").getStatus());
        assertEquals(0, service.listAssignedTo("rev1", AssignmentStatus.ASIGNADA, 1, 10).totalItems());
    }

    @Test
    void hu05PartialEvaluationDoesNotCloseReview() {
        service.assignReviewers("q1", List.of("rev1", "rev2"), "admin");
        publisher.events.clear();
        service.submitEvaluation("q1", "rev1", new EvaluationCommand(Verdict.APROBADA, ReviewFixtures.allScores(4), null));
        assertEquals(List.of("EVALUATED:rev1:APROBADA"), publisher.events);
        assertEquals(ReviewStatus.EN_REVISION, service.get("q1").getStatus());
    }

    @Test
    void hu05ObservationsAreVisibleToAuthorButNotToStrangers() {
        service.assignReviewers("q1", List.of("rev1"), "admin");
        service.addObservation("q1", "rev1", "Ajustar el contexto");
        assertEquals(1, service.observationsFor("q1", "autor1").size());
        assertEquals(1, service.observationsFor("q1", "admin").size());
        assertEquals(Kind.FORBIDDEN, assertThrows(ReviewDomainException.class,
                () -> service.observationsFor("q1", "rev2")).getKind());
        assertTrue(publisher.events.contains("OBSERVATION:rev1"));
    }

    @Test
    void unknownQuestionIsNotFound() {
        assertEquals(Kind.NOT_FOUND, assertThrows(ReviewDomainException.class, () -> service.get("nope")).getKind());
    }
}
