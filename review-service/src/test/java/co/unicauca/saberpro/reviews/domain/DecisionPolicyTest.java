package co.unicauca.saberpro.reviews.domain;

import co.unicauca.saberpro.reviews.domain.model.QuestionReview;
import co.unicauca.saberpro.reviews.domain.model.Verdict;
import co.unicauca.saberpro.reviews.domain.policy.DecisionPolicy;
import co.unicauca.saberpro.reviews.domain.policy.MajorityPolicy;
import co.unicauca.saberpro.reviews.domain.policy.UnanimousApprovalPolicy;
import co.unicauca.saberpro.reviews.testdoubles.ReviewFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del patron Strategy: misma secuencia de conceptos, distinta decision. */
class DecisionPolicyTest {

    private static Optional<Verdict> run(DecisionPolicy policy, Verdict... verdicts) {
        QuestionReview r = ReviewFixtures.pending("q");
        r.assignReviewers(List.of("rev1", "rev2", "rev3"), "admin");
        Optional<Verdict> last = Optional.empty();
        String[] reviewers = {"rev1", "rev2", "rev3"};
        for (int i = 0; i < verdicts.length; i++) {
            last = r.submitEvaluation(reviewers[i], verdicts[i], ReviewFixtures.allScores(3), null, policy);
        }
        return last;
    }

    @Test
    void unanimousRejectsOnFirstRejection() {
        assertEquals(Optional.of(Verdict.RECHAZADA), run(new UnanimousApprovalPolicy(), Verdict.RECHAZADA));
    }

    @Test
    void unanimousWaitsForEveryApproval() {
        assertEquals(Optional.empty(), run(new UnanimousApprovalPolicy(), Verdict.APROBADA, Verdict.APROBADA));
        assertEquals(Optional.of(Verdict.APROBADA),
                run(new UnanimousApprovalPolicy(), Verdict.APROBADA, Verdict.APROBADA, Verdict.APROBADA));
    }

    @Test
    void majorityDecidesWithTwoOfThree() {
        assertEquals(Optional.empty(), run(new MajorityPolicy(), Verdict.APROBADA));
        assertEquals(Optional.of(Verdict.APROBADA), run(new MajorityPolicy(), Verdict.APROBADA, Verdict.APROBADA));
        assertEquals(Optional.empty(), run(new MajorityPolicy(), Verdict.APROBADA, Verdict.RECHAZADA));
        assertEquals(Optional.of(Verdict.RECHAZADA),
                run(new MajorityPolicy(), Verdict.APROBADA, Verdict.RECHAZADA, Verdict.RECHAZADA));
    }

    @Test
    void majorityTieWithTwoReviewersRejects() {
        QuestionReview r = ReviewFixtures.pending("q");
        r.assignReviewers(List.of("rev1", "rev2"), "admin");
        MajorityPolicy policy = new MajorityPolicy();
        r.submitEvaluation("rev1", Verdict.APROBADA, ReviewFixtures.allScores(4), null, policy);
        assertEquals(Optional.of(Verdict.RECHAZADA),
                r.submitEvaluation("rev2", Verdict.RECHAZADA, ReviewFixtures.allScores(2), null, policy));
    }

    @Test
    void emptyAssignmentsHaveNoDecision() {
        assertTrue(new MajorityPolicy().decide(List.of()).isEmpty());
        assertTrue(new UnanimousApprovalPolicy().decide(List.of()).isEmpty());
        assertEquals("MAJORITY", new MajorityPolicy().name());
        assertEquals("UNANIMOUS", new UnanimousApprovalPolicy().name());
    }
}
