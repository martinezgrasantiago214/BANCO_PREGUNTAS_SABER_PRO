package co.unicauca.saberpro.review.infrastructure.adapter.out.persistence;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.review.domain.exception.ConcurrentReviewUpdateException;
import co.unicauca.saberpro.review.domain.model.QuestionReview;
import co.unicauca.saberpro.review.domain.model.ReviewRound;
import co.unicauca.saberpro.review.domain.model.ReviewStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.ADMIN;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.QUESTION;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.T0;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.candidates;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.submission;
import static org.junit.jupiter.api.Assertions.*;

/** Adaptador SQLite: el agregado completo sobrevive a un reinicio y el bloqueo optimista funciona. */
class SqliteQuestionReviewRepositoryTest {

    @TempDir
    Path tempDir;

    private SqliteQuestionReviewRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SqliteQuestionReviewRepository("jdbc:sqlite:" + tempDir.resolve("review.db"));
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    private QuestionReview reload() {
        repository.close();
        repository = new SqliteQuestionReviewRepository("jdbc:sqlite:" + tempDir.resolve("review.db"));
        return repository.findById(QUESTION).orElseThrow();
    }

    @Test
    void guardaYReconstruyeElAgregadoConTodasSusRondas() {
        QuestionReview review = QuestionReview.open(submission(1));
        repository.save(review);

        review = repository.findById(QUESTION).orElseThrow();
        review.assignReviewers(candidates("rev1", "rev2"), ADMIN, T0.plusSeconds(10));
        repository.save(review);

        review = repository.findById(QUESTION).orElseThrow();
        review.evaluate("rev2", EvaluationVerdict.RECHAZADA, "Ajustar el distractor B", T0.plusSeconds(20));
        repository.save(review);

        review = repository.findById(QUESTION).orElseThrow();
        review.receive(submission(2));
        repository.save(review);

        QuestionReview stored = reload();
        assertEquals(4, stored.version());
        assertEquals(2, stored.rounds().size());
        ReviewRound first = stored.rounds().get(0);
        assertEquals(ReviewStatus.RECHAZADA, first.status());
        assertEquals(java.util.List.of("rev1", "rev2"), first.reviewerLogins());
        assertEquals("rev2", first.evaluation().orElseThrow().reviewerLogin());
        assertEquals("Ajustar el distractor B", first.evaluation().orElseThrow().observations());
        assertEquals(ReviewStatus.PENDIENTE_ASIGNACION, stored.status());
        assertEquals(2, stored.currentRound().questionVersion());
    }

    @Test
    void consultaLasPendientesYLasDeUnRevisor() {
        QuestionReview review = QuestionReview.open(submission(1));
        review.assignReviewers(candidates("rev1"), ADMIN, T0);
        repository.save(review);

        assertEquals(1, repository.findAwaitingDecision().size());
        assertEquals(1, repository.findByReviewer("rev1").size());
        assertTrue(repository.findByReviewer("rev2").isEmpty());

        review = repository.findById(QUESTION).orElseThrow();
        review.evaluate("rev1", EvaluationVerdict.APROBADA, "", T0.plusSeconds(5));
        repository.save(review);
        assertTrue(repository.findAwaitingDecision().isEmpty());
    }

    @Test
    void detectaEscriturasSimultaneas() {
        repository.save(QuestionReview.open(submission(1)));
        QuestionReview a = repository.findById(QUESTION).orElseThrow();
        QuestionReview b = repository.findById(QUESTION).orElseThrow();

        a.assignReviewers(candidates("rev1"), ADMIN, T0);
        repository.save(a);
        b.assignReviewers(candidates("rev2"), ADMIN, T0);

        assertThrows(ConcurrentReviewUpdateException.class, () -> repository.save(b));
        assertEquals(java.util.List.of("rev1"), reload().currentRound().reviewerLogins());
    }

    @Test
    void dosAperturasDeLaMismaPreguntaNoSeDuplican() {
        repository.save(QuestionReview.open(submission(1)));
        assertThrows(ConcurrentReviewUpdateException.class, () -> repository.save(QuestionReview.open(submission(1))));
    }

    @Test
    void unaPreguntaSinRevisionNoExiste() {
        assertTrue(repository.findById("nada").isEmpty());
    }
}
