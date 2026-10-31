package co.unicauca.saberpro.reviews.domain;

import co.unicauca.saberpro.reviews.domain.exception.ReviewDomainException;
import co.unicauca.saberpro.reviews.domain.model.*;
import co.unicauca.saberpro.reviews.domain.policy.UnanimousApprovalPolicy;
import co.unicauca.saberpro.reviews.testdoubles.ReviewFixtures;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReviewerAssignmentTest {

    private QuestionReview inReview() {
        QuestionReview r = ReviewFixtures.pending("q1");
        r.assignReviewers(List.of("rev1", "rev2"), "admin");
        return r;
    }

    @Test
    void newAssignmentIsPending() {
        ReviewerAssignment a = new ReviewerAssignment("rev1", Instant.now());
        assertEquals(AssignmentStatus.ASIGNADA, a.getStatus());
        assertFalse(a.isCompleted());
        assertEquals(0.0, a.averageScore());
    }

    @Test
    void mustScoreEveryCriterionExactlyOnce() {
        QuestionReview r = inReview();
        List<CriterionScore> incomplete = List.of(new CriterionScore(ReviewCriterion.CLARIDAD_ENUNCIADO, 4, ""));
        assertThrows(ReviewDomainException.class, () -> r.submitEvaluation("rev1", Verdict.APROBADA, incomplete,
                null, new UnanimousApprovalPolicy()));
        List<CriterionScore> duplicated = List.of(
                new CriterionScore(ReviewCriterion.CLARIDAD_ENUNCIADO, 4, ""),
                new CriterionScore(ReviewCriterion.CLARIDAD_ENUNCIADO, 3, ""));
        assertThrows(ReviewDomainException.class, () -> r.submitEvaluation("rev1", Verdict.APROBADA, duplicated,
                null, new UnanimousApprovalPolicy()));
    }

    @Test
    void verdictIsRequiredAndCannotEvaluateTwice() {
        QuestionReview r = inReview();
        assertThrows(ReviewDomainException.class, () -> r.submitEvaluation("rev1", null,
                ReviewFixtures.allScores(4), null, new UnanimousApprovalPolicy()));
        r.submitEvaluation("rev1", Verdict.APROBADA, ReviewFixtures.allScores(4), null, new UnanimousApprovalPolicy());
        ReviewDomainException ex = assertThrows(ReviewDomainException.class, () -> r.submitEvaluation("rev1",
                Verdict.APROBADA, ReviewFixtures.allScores(4), null, new UnanimousApprovalPolicy()));
        assertEquals(ReviewDomainException.Kind.INVALID_STATE, ex.getKind());
    }

    @Test
    void computesAverageScore() {
        QuestionReview r = inReview();
        r.submitEvaluation("rev1", Verdict.APROBADA, ReviewFixtures.allScores(4), null, new UnanimousApprovalPolicy());
        ReviewerAssignment a = r.getAssignments().get(0);
        assertEquals(4.0, a.averageScore());
        assertNotNull(a.getCompletedAt());
    }
}
