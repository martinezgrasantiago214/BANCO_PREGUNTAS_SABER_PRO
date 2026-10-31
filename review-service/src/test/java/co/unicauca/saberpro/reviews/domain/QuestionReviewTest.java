package co.unicauca.saberpro.reviews.domain;

import co.unicauca.saberpro.reviews.domain.exception.ReviewDomainException;
import co.unicauca.saberpro.reviews.domain.exception.ReviewDomainException.Kind;
import co.unicauca.saberpro.reviews.domain.model.*;
import co.unicauca.saberpro.reviews.domain.policy.UnanimousApprovalPolicy;
import co.unicauca.saberpro.reviews.testdoubles.ReviewFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class QuestionReviewTest {

    private final UnanimousApprovalPolicy policy = new UnanimousApprovalPolicy();

    @Test
    void startsPendingAssignment() {
        QuestionReview r = ReviewFixtures.pending("q1");
        assertEquals(ReviewStatus.PENDIENTE_ASIGNACION, r.getStatus());
        assertEquals(1, r.getRound());
    }

    @Test
    @DisplayName("HU04: entre 1 y 3 revisores distintos")
    void assignsBetweenOneAndThreeReviewers() {
        assertEquals(Kind.VALIDATION, assertThrows(ReviewDomainException.class,
                () -> ReviewFixtures.pending("q").assignReviewers(List.of(), "admin")).getKind());
        assertEquals(Kind.VALIDATION, assertThrows(ReviewDomainException.class,
                () -> ReviewFixtures.pending("q").assignReviewers(List.of("r1", "r2", "r3", "r4"), "admin")).getKind());

        QuestionReview r = ReviewFixtures.pending("q");
        r.assignReviewers(List.of("rev1", "rev2", "rev3"), "admin");
        assertEquals(ReviewStatus.EN_REVISION, r.getStatus());
        assertEquals(List.of("rev1", "rev2", "rev3"), r.reviewerLogins());
        assertEquals("admin", r.getAssignedBy());
        assertNotNull(r.getAssignedAt());
    }

    @Test
    void duplicatedLoginsCountOnce() {
        QuestionReview r = ReviewFixtures.pending("q");
        r.assignReviewers(List.of("rev1", " rev1 ", "rev2"), "admin");
        assertEquals(2, r.getAssignments().size());
    }

    @Test
    void authorCannotReviewOwnQuestion() {
        assertThrows(ReviewDomainException.class,
                () -> ReviewFixtures.pending("q").assignReviewers(List.of("autor1"), "admin"));
    }

    @Test
    void cannotAssignTwice() {
        QuestionReview r = ReviewFixtures.pending("q");
        r.assignReviewers(List.of("rev1"), "admin");
        assertEquals(Kind.INVALID_STATE,
                assertThrows(ReviewDomainException.class, () -> r.assignReviewers(List.of("rev2"), "admin")).getKind());
    }

    @Test
    @DisplayName("HU05: la evaluacion con observacion cierra la revision segun la politica")
    void evaluationWithObservationClosesReview() {
        QuestionReview r = ReviewFixtures.pending("q");
        r.assignReviewers(List.of("rev1"), "admin");
        Optional<Verdict> decision = r.submitEvaluation("rev1", Verdict.RECHAZADA, ReviewFixtures.allScores(2),
                "El distractor B es inverosímil.", policy);
        assertEquals(Optional.of(Verdict.RECHAZADA), decision);
        assertEquals(ReviewStatus.RECHAZADA, r.getStatus());
        assertEquals(1, r.getObservations().size());
    }

    @Test
    void onlyAssignedReviewerCanEvaluateOrObserve() {
        QuestionReview r = ReviewFixtures.pending("q");
        r.assignReviewers(List.of("rev1"), "admin");
        assertEquals(Kind.FORBIDDEN, assertThrows(ReviewDomainException.class,
                () -> r.addObservation("rev2", "Hola")).getKind());
        assertEquals(Kind.FORBIDDEN, assertThrows(ReviewDomainException.class,
                () -> r.submitEvaluation("rev2", Verdict.APROBADA, ReviewFixtures.allScores(5), null, policy)).getKind());
        assertEquals("rev1", r.addObservation("rev1", "Revisar la bibliografía").reviewerLogin());
        assertTrue(r.isAssignedTo("rev1"));
    }

    @Test
    void cannotEvaluateBeforeAssignment() {
        QuestionReview r = ReviewFixtures.pending("q");
        assertEquals(Kind.INVALID_STATE, assertThrows(ReviewDomainException.class,
                () -> r.addObservation("rev1", "x")).getKind());
    }

    @Test
    void rejectedQuestionCanBeResubmittedAsNewRound() {
        QuestionReview r = ReviewFixtures.pending("q");
        assertFalse(r.resubmit("x", "c", "t"), "un reenvio mientras esta pendiente es un evento repetido");
        r.assignReviewers(List.of("rev1"), "admin");
        r.submitEvaluation("rev1", Verdict.RECHAZADA, ReviewFixtures.allScores(1), "Corregir", policy);
        assertTrue(r.resubmit("Nuevo enunciado corregido", "c", "t"));
        assertEquals(ReviewStatus.PENDIENTE_ASIGNACION, r.getStatus());
        assertEquals(2, r.getRound());
        assertTrue(r.getAssignments().isEmpty());
        assertEquals(1, r.getObservations().size(), "las observaciones previas se conservan como historial");
    }
}
