package co.unicauca.saberpro.review.domain.model;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.review.domain.event.QuestionEvaluated;
import co.unicauca.saberpro.review.domain.event.ReviewDomainEvent;
import co.unicauca.saberpro.review.domain.event.ReviewRoundReopened;
import co.unicauca.saberpro.review.domain.event.ReviewersAssigned;
import co.unicauca.saberpro.review.domain.exception.ReviewConflictException;
import co.unicauca.saberpro.review.domain.exception.ReviewRuleViolationException;
import co.unicauca.saberpro.review.domain.exception.ReviewerNotAllowedException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.ADMIN;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.AUTHOR;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.QUESTION;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.T0;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.candidates;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.submission;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Agregado QuestionReview (DDD): las reglas de HU04, HU05 y HU06 viven aqui
 * y se prueban sin base de datos, sin Spring y sin broker.
 */
class QuestionReviewTest {

    private static final Instant T1 = T0.plusSeconds(60);
    private static final Instant T2 = T0.plusSeconds(120);

    private static QuestionReview opened() {
        QuestionReview review = QuestionReview.open(submission(1));
        review.pullDomainEvents();
        return review;
    }

    private static QuestionReview inReview(String... reviewers) {
        QuestionReview review = opened();
        review.assignReviewers(candidates(reviewers), ADMIN, T1);
        review.pullDomainEvents();
        return review;
    }

    private static QuestionReview rejected() {
        QuestionReview review = inReview("rev1", "rev2");
        review.evaluate("rev1", EvaluationVerdict.RECHAZADA, "El distractor C tambien es correcto.", T2);
        review.pullDomainEvents();
        return review;
    }

    private static List<String> messages(Runnable action, Class<? extends RuntimeException> type) {
        RuntimeException ex = assertThrows(type, action::run);
        return ex instanceof co.unicauca.saberpro.review.domain.exception.ReviewDomainException d
                ? d.getMessages() : List.of(ex.getMessage());
    }

    // ------------------------------------------------------------ apertura

    @Test
    void unaPreguntaRecienEnviadaAbreLaRonda1PendienteDeAsignacion() {
        QuestionReview review = QuestionReview.open(submission(1));

        assertEquals(QUESTION, review.questionId());
        assertEquals(AUTHOR, review.authorLogin());
        assertEquals(1, review.rounds().size());
        assertEquals(1, review.currentRound().number());
        assertEquals(1, review.currentRound().questionVersion());
        assertEquals(ReviewStatus.PENDIENTE_ASIGNACION, review.status());
        assertTrue(review.isAwaitingDecision());
        assertTrue(review.pullDomainEvents().isEmpty());
    }

    @Test
    void recibirOtraVezLaMismaVersionNoHaceNada() {
        QuestionReview review = opened();

        assertFalse(review.receive(submission(1)));
        assertEquals(1, review.rounds().size());
        assertTrue(review.pullDomainEvents().isEmpty());
    }

    // ------------------------------------------------------------ HU04 asignar

    @Test
    void hu04AsignarPasaLaRondaAEnRevisionYRegistraElEvento() {
        QuestionReview review = opened();

        List<ReviewerAssignment> created = review.assignReviewers(candidates("rev1", "rev2"), ADMIN, T1);

        assertEquals(List.of("rev1", "rev2"), created.stream().map(ReviewerAssignment::reviewerLogin).toList());
        assertEquals(ReviewStatus.EN_REVISION, review.status());
        assertTrue(review.isAwaitingDecision());
        List<ReviewDomainEvent> events = review.pullDomainEvents();
        assertEquals(1, events.size());
        ReviewersAssigned event = (ReviewersAssigned) events.get(0);
        assertEquals(List.of("rev1", "rev2"), event.reviewerLogins());
        assertEquals(ADMIN, event.assignedBy());
        assertEquals(1, event.round());
        assertTrue(review.pullDomainEvents().isEmpty(), "los eventos se entregan una sola vez");
    }

    @Test
    void hu04SePuedenAgregarRevisoresHastaTres() {
        QuestionReview review = inReview("rev1");

        review.assignReviewers(candidates("rev2", "rev3"), ADMIN, T2);

        assertEquals(List.of("rev1", "rev2", "rev3"), review.currentRound().reviewerLogins());
        assertEquals(0, review.currentRound().remainingReviewerSlots());
    }

    @Test
    void hu04NoSePuedeSuperarElMaximoDeTresSumandoLosQueYaTenia() {
        QuestionReview review = inReview("rev1", "rev2");

        List<String> errors = messages(() -> review.assignReviewers(candidates("rev3", "rev4"), ADMIN, T2),
                ReviewRuleViolationException.class);

        assertTrue(errors.get(0).contains("no puede tener mas de 3"), errors.toString());
        assertEquals(2, review.currentRound().assignments().size(), "no se asigno ninguno");
    }

    @Test
    void hu04LaSeleccionDebeTenerEntreUnoYTresRevisoresDistintos() {
        QuestionReview review = opened();

        assertThrows(ReviewRuleViolationException.class, () -> review.assignReviewers(List.of(), ADMIN, T1));
        assertThrows(ReviewRuleViolationException.class,
                () -> review.assignReviewers(candidates("rev1", "rev2", "rev3", "rev4"), ADMIN, T1));
        assertThrows(ReviewRuleViolationException.class,
                () -> review.assignReviewers(candidates("rev1", "rev1"), ADMIN, T1));
        assertEquals(ReviewStatus.PENDIENTE_ASIGNACION, review.status());
    }

    @Test
    void hu04SoloRevisoresExistentesYNuncaElAutorReportandoTodosLosErroresJuntos() {
        QuestionReview review = opened();

        List<String> errors = messages(() -> review.assignReviewers(candidates(AUTHOR, "fantasma", ADMIN), ADMIN, T1),
                ReviewRuleViolationException.class);

        assertEquals(3, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("autor"));
        assertTrue(errors.get(1).contains("no existe"));
        assertTrue(errors.get(2).contains("rol Revisor"));
        assertTrue(review.pullDomainEvents().isEmpty());
    }

    @Test
    void hu04UnRevisorNoSeAsignaDosVecesALaMismaRonda() {
        QuestionReview review = inReview("rev1");

        List<String> errors = messages(() -> review.assignReviewers(candidates("rev1"), ADMIN, T2),
                ReviewRuleViolationException.class);

        assertTrue(errors.get(0).contains("ya esta asignado"));
    }

    @Test
    void hu04ElAdministradorEsObligatorio() {
        assertThrows(ReviewRuleViolationException.class,
                () -> opened().assignReviewers(candidates("rev1"), " ", T1));
    }

    @Test
    void hu04NoSeAsignanRevisoresAUnaRondaYaDecidida() {
        QuestionReview review = rejected();

        assertThrows(ReviewConflictException.class, () -> review.assignReviewers(candidates("rev3"), ADMIN, T2));
    }

    // ------------------------------------------------------------ HU05 evaluar

    @Test
    void hu05ElPrimerRevisorQueEvaluaDecideLaRonda() {
        QuestionReview review = inReview("rev1", "rev2");

        Evaluation evaluation = review.evaluate("rev2", EvaluationVerdict.APROBADA, "  Bien construida.  ", T2);

        assertEquals("rev2", evaluation.reviewerLogin());
        assertEquals("Bien construida.", evaluation.observations());
        assertEquals(ReviewStatus.APROBADA, review.status());
        assertFalse(review.isAwaitingDecision());
        QuestionEvaluated event = (QuestionEvaluated) review.pullDomainEvents().get(0);
        assertEquals(EvaluationVerdict.APROBADA, event.verdict());
        assertEquals(1, event.questionVersion());
        assertEquals(AUTHOR, event.authorLogin());
    }

    @Test
    void hu05DespuesDelVeredictoNadieMasPuedeEvaluar() {
        QuestionReview review = inReview("rev1", "rev2");
        review.evaluate("rev1", EvaluationVerdict.APROBADA, "", T2);

        List<String> errors = messages(() -> review.evaluate("rev2", EvaluationVerdict.RECHAZADA, "No", T2),
                ReviewConflictException.class);

        assertTrue(errors.get(0).contains("ya fue evaluada por rev1"));
        assertEquals(1, review.evaluations().size());
    }

    @Test
    void hu05SoloEvaluaUnRevisorAsignado() {
        QuestionReview review = inReview("rev1");

        assertThrows(ReviewerNotAllowedException.class,
                () -> review.evaluate("rev2", EvaluationVerdict.APROBADA, "", T2));
        assertEquals(ReviewStatus.EN_REVISION, review.status());
    }

    @Test
    void hu05SinRevisoresAsignadosNoSePuedeEvaluar() {
        assertThrows(ReviewerNotAllowedException.class,
                () -> opened().evaluate("rev1", EvaluationVerdict.APROBADA, "", T2));
    }

    @Test
    void hu05RechazarExigeObservaciones() {
        QuestionReview review = inReview("rev1");

        List<String> errors = messages(() -> review.evaluate("rev1", EvaluationVerdict.RECHAZADA, "   ", T2),
                ReviewRuleViolationException.class);

        assertTrue(errors.get(0).contains("observaciones"));
        assertEquals(ReviewStatus.EN_REVISION, review.status());
    }

    @Test
    void hu05ElVeredictoEsObligatorio() {
        assertThrows(ReviewRuleViolationException.class,
                () -> inReview("rev1").evaluate("rev1", null, "Algo", T2));
    }

    // ------------------------------------------------------------ HU06 nueva version

    @Test
    void hu06UnaVersionMejoradaDespuesDelRechazoAbreUnaRondaNueva() {
        QuestionReview review = rejected();

        assertTrue(review.receive(submission(2)));

        assertEquals(2, review.rounds().size());
        ReviewRound round2 = review.currentRound();
        assertEquals(2, round2.number());
        assertEquals(2, round2.questionVersion());
        assertEquals(ReviewStatus.PENDIENTE_ASIGNACION, round2.status());
        assertTrue(round2.assignments().isEmpty());
        assertEquals(ReviewStatus.RECHAZADA, review.rounds().get(0).status(), "la ronda 1 queda en el historial");

        ReviewRoundReopened event = (ReviewRoundReopened) review.pullDomainEvents().get(0);
        assertEquals(List.of("rev1", "rev2"), event.previousReviewers());
        assertEquals("El distractor C tambien es correcto.", event.previousObservations());
        assertEquals(2, event.round());
    }

    @Test
    void hu06NoSeAbreUnaRondaNuevaSiLaActualSigueAbiertaOFueAprobada() {
        assertThrows(ReviewConflictException.class, () -> inReview("rev1").receive(submission(2)));

        QuestionReview approved = inReview("rev1");
        approved.evaluate("rev1", EvaluationVerdict.APROBADA, "", T2);
        assertThrows(ReviewConflictException.class, () -> approved.receive(submission(2)));
    }

    @Test
    void hu06EnLaRondaNuevaSoloEvaluanLosRevisoresDeEsaRonda() {
        QuestionReview review = rejected();
        review.receive(submission(2));
        review.assignReviewers(candidates("rev3"), ADMIN, T2.plusSeconds(10));

        List<String> errors = messages(() -> review.evaluate("rev1", EvaluationVerdict.APROBADA, "", T2),
                ReviewerNotAllowedException.class);
        assertTrue(errors.get(0).contains("ronda 2"), errors.toString());

        review.evaluate("rev3", EvaluationVerdict.APROBADA, "Corregida.", T2.plusSeconds(20));
        assertEquals(ReviewStatus.APROBADA, review.status());
        assertEquals(2, review.evaluations().size(), "el historial conserva ambas evaluaciones");
    }

    @Test
    void hu06ElAutorPuedeVolverAAsignarAlRevisorQueLaRechazo() {
        QuestionReview review = rejected();
        review.receive(submission(2));

        review.assignReviewers(candidates("rev1"), ADMIN, T2);

        assertEquals(List.of("rev1"), review.currentRound().reviewerLogins());
    }

    @Test
    void hu06ElHistorialLoVenElAutorLosAdministradoresYLosRevisores() {
        QuestionReview review = rejected();

        assertTrue(review.canBeConsultedBy(AUTHOR, false));
        assertTrue(review.canBeConsultedBy("rev2", false));
        assertTrue(review.canBeConsultedBy("cualquiera", true));
        assertFalse(review.canBeConsultedBy("rev3", false));
        assertFalse(review.canBeConsultedBy(null, false));
    }

    @Test
    void unEnvioDeOtraPreguntaEsUnErrorDeProgramacion() {
        assertThrows(IllegalArgumentException.class, () -> opened().receive(
                new QuestionSubmission("otra", 2, AUTHOR, "¿?", T0)));
    }
}
