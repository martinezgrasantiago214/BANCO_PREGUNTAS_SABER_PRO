package co.unicauca.saberpro.question.access;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.question.TestData;
import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.factory.MultipleChoiceQuestionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Persistencia SQLite del question-service (con una base temporal). */
class SQLiteQuestionRepositoryTest {

    @TempDir
    Path tempDir;

    private SQLiteQuestionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SQLiteQuestionRepository("jdbc:sqlite:" + tempDir.resolve("test.db"));
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    private Question nueva(String author) {
        return Question.builder().type(MultipleChoiceQuestionFactory.TYPE)
                .fromRequest(TestData.validRequest("x")).authorLogin(author).build();
    }

    @Test
    void guardaYRecuperaUnaPreguntaConTodosSusDatos() {
        Question q = nueva("autor1");
        assertTrue(repository.save(q));

        Question leida = repository.findById(q.getId()).orElseThrow();
        assertEquals(q.getDirectQuestion(), leida.getDirectQuestion());
        assertEquals(q.getDistractors(), leida.getDistractors());
        assertEquals(q.getDifficultyLevel(), leida.getDifficultyLevel());
        assertEquals(QuestionStatus.BORRADOR, leida.getStatus());
        assertEquals(q.getCreatedAt(), leida.getCreatedAt());
    }

    @Test
    void actualizaElEstado() {
        Question q = nueva("autor1");
        repository.save(q);
        q.submitForReview();

        assertTrue(repository.update(q));
        assertEquals(QuestionStatus.PENDIENTE_REVISION, repository.findById(q.getId()).orElseThrow().getStatus());
    }

    @Test
    void buscaPorAutorYEstadoYPagina() {
        for (int i = 0; i < 7; i++) {
            repository.save(nueva("autor1"));
        }
        repository.save(nueva("autor2"));

        PagedResult<Question> pagina2 = repository.search(new QuestionFilterCriteria()
                .setAuthorLogin("autor1").setStatus(QuestionStatus.BORRADOR).setPage(2).setPageSize(5));

        assertEquals(7, pagina2.totalItems());
        assertEquals(2, pagina2.items().size());
        assertEquals(2, pagina2.totalPages());
    }

    @Test
    void buscaPorPalabraClaveYTema() {
        repository.save(nueva("autor1"));

        assertEquals(1, repository.search(new QuestionFilterCriteria().setKeyword("plugins")).totalItems());
        assertEquals(1, repository.search(new QuestionFilterCriteria().setTopic("Patrones")).totalItems());
        assertEquals(0, repository.search(new QuestionFilterCriteria().setKeyword("inexistente")).totalItems());
    }

    @Test
    void unaPreguntaInexistenteDevuelveVacio() {
        assertTrue(repository.findById("no-existe").isEmpty());
    }
}
