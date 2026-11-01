package co.unicauca.saberpro.question.application;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.common.web.ResourceNotFoundException;
import co.unicauca.saberpro.question.TestData;
import co.unicauca.saberpro.question.access.PagedResult;
import co.unicauca.saberpro.question.access.QuestionFilterCriteria;
import co.unicauca.saberpro.question.access.SQLiteQuestionRepository;
import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.QuestionRequest;
import co.unicauca.saberpro.question.domain.factory.QuestionFactoryRegistry;
import co.unicauca.saberpro.question.domain.factory.QuestionValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU03 (PSP-9): listar, buscar, filtrar, paginar y editar las preguntas del
 * autor. Pruebas del servicio de aplicacion con el repositorio SQLite real
 * sobre una base temporal (PSP-29).
 */
class ListAndEditQuestionsTest {

    private static final String MC = "MULTIPLE_CHOICE";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 1, 8, 0);

    @TempDir
    Path tempDir;

    private SQLiteQuestionRepository repository;
    private QuestionApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new SQLiteQuestionRepository("jdbc:sqlite:" + tempDir.resolve("hu03.db"));
        service = new QuestionApplicationService(repository, new QuestionFactoryRegistry(), new DomainEventBus());
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    /** Guarda una pregunta del autor con el tema, enunciado y estado indicados; minutes ordena la creacion. */
    private Question saved(String author, String topic, String directQuestion, QuestionStatus status, int minutes) {
        Question q = Question.builder().type(MC).fromRequest(TestData.validRequest(MC))
                .authorLogin(author).topic(topic).directQuestion(directQuestion)
                .status(status).createdAt(T0.plusMinutes(minutes)).build();
        assertTrue(repository.save(q));
        return q;
    }

    private static QuestionFilterCriteria mine(String author) {
        return new QuestionFilterCriteria().setAuthorLogin(author).setPageSize(10);
    }

    private static List<String> ids(PagedResult<Question> page) {
        return page.items().stream().map(Question::getId).toList();
    }

    @Nested
    class Listado {

        @Test
        void elAutorSoloVeSusPreguntasDeLaMasRecienteALaMasAntigua() {
            Question vieja = saved("autor1", "Arquitectura", "¿Pregunta vieja del autor?", QuestionStatus.BORRADOR, 1);
            saved("autor2", "Arquitectura", "¿Pregunta de otro autor?", QuestionStatus.BORRADOR, 2);
            Question nueva = saved("autor1", "Patrones", "¿Pregunta nueva del autor?", QuestionStatus.APROBADA, 3);

            PagedResult<Question> page = service.search(mine("autor1"));

            assertEquals(List.of(nueva.getId(), vieja.getId()), ids(page));
            assertEquals(2, page.totalItems());
        }

        @Test
        void sinPreguntasDevuelveUnaPaginaVacia() {
            PagedResult<Question> page = service.search(mine("autor-sin-preguntas"));

            assertTrue(page.items().isEmpty());
            assertEquals(0, page.totalItems());
            assertEquals(1, page.totalPages());
        }
    }

    @Nested
    class BusquedaYFiltros {

        @BeforeEach
        void datos() {
            saved("autor1", "Arquitectura", "¿Que estilo favorece la escalabilidad?", QuestionStatus.BORRADOR, 1);
            saved("autor1", "Arquitectura", "¿Que es un microservicio?", QuestionStatus.APROBADA, 2);
            saved("autor1", "Patrones GoF", "¿Que resuelve el patron Observer?", QuestionStatus.BORRADOR, 3);
            saved("autor2", "Arquitectura", "¿Que estilo usa eventos para escalar?", QuestionStatus.BORRADOR, 4);
        }

        @Test
        void buscaPorPalabraClaveEnElEnunciado() {
            PagedResult<Question> page = service.search(mine("autor1").setKeyword("escalabilidad"));

            assertEquals(1, page.totalItems());
            assertEquals("¿Que estilo favorece la escalabilidad?", page.items().get(0).getDirectQuestion());
        }

        @Test
        void filtraPorEstado() {
            PagedResult<Question> page = service.search(mine("autor1").setStatus(QuestionStatus.BORRADOR));

            assertEquals(2, page.totalItems());
            assertTrue(page.items().stream().allMatch(q -> q.getStatus() == QuestionStatus.BORRADOR));
        }

        @Test
        void filtraPorTema() {
            PagedResult<Question> page = service.search(mine("autor1").setTopic("Patrones"));

            assertEquals(1, page.totalItems());
            assertEquals("Patrones GoF", page.items().get(0).getTopic());
        }

        @Test
        void combinaFiltrosYBusqueda() {
            PagedResult<Question> page = service.search(mine("autor1").setTopic("Arquitectura")
                    .setStatus(QuestionStatus.APROBADA).setKeyword("microservicio"));

            assertEquals(1, page.totalItems());
            assertEquals("¿Que es un microservicio?", page.items().get(0).getDirectQuestion());
        }

        @Test
        void losFiltrosEnBlancoSeIgnoranYLosTextosSeRecortan() {
            assertEquals(3, service.search(mine("autor1").setTopic("   ").setKeyword("")).totalItems());
            assertEquals(1, service.search(mine("autor1").setKeyword("  Observer  ")).totalItems());
        }
    }

    @Nested
    class Paginacion {

        @BeforeEach
        void doce() {
            for (int i = 1; i <= 12; i++) {
                saved("autor1", "Tema", String.format("¿Pregunta numero %02d?", i), QuestionStatus.BORRADOR, i);
            }
        }

        @Test
        void partePorPaginasEInformaElTotal() {
            PagedResult<Question> first = service.search(mine("autor1").setPageSize(5).setPage(1));
            PagedResult<Question> last = service.search(mine("autor1").setPageSize(5).setPage(3));

            assertEquals(5, first.items().size());
            assertEquals("¿Pregunta numero 12?", first.items().get(0).getDirectQuestion());
            assertEquals(2, last.items().size());
            assertEquals("¿Pregunta numero 01?", last.items().get(1).getDirectQuestion());
            assertEquals(12, first.totalItems());
            assertEquals(3, first.totalPages());
        }

        @Test
        void unaPaginaQueYaNoExisteDevuelveLaUltima() {
            PagedResult<Question> page = service.search(mine("autor1").setPageSize(5).setPage(9));

            assertEquals(3, page.page());
            assertEquals(2, page.items().size());
        }

        @Test
        void elTamanoDePaginaTieneUnTope() {
            PagedResult<Question> page = service.search(mine("autor1").setPageSize(10_000));

            assertEquals(QuestionApplicationService.MAX_PAGE_SIZE, page.pageSize());
        }
    }

    @Nested
    class Edicion {

        private QuestionRequest cambios(String author) {
            return new QuestionRequest(MC, "Contexto actualizado por el autor.",
                    "¿Que patron permite agregar tipos de pregunta sin tocar el nucleo?",
                    List.of("Microkernel", "Capas", "Broker", "Pipes and filters"), "Microkernel",
                    "El microkernel carga los plugins sin modificar el nucleo.",
                    "Richards, M. Software Architecture Patterns. O'Reilly, 2015.",
                    "Arquitectura de software", "Estilos", "Microkernel", DifficultyLevel.ALTO, author);
        }

        @Test
        void elAutorEditaSuBorradorYConservaIdTipoFechaYEstado() {
            Question original = saved("autor1", "Tema", "¿Pregunta original del autor?", QuestionStatus.BORRADOR, 1);

            Question updated = service.update(original.getId(), cambios("autor1"));

            Question stored = service.findById(original.getId());
            assertEquals("¿Que patron permite agregar tipos de pregunta sin tocar el nucleo?", stored.getDirectQuestion());
            assertEquals("Estilos", stored.getTopic());
            assertEquals(DifficultyLevel.ALTO, stored.getDifficultyLevel());
            assertEquals(original.getId(), updated.getId());
            assertEquals(MC, stored.getType());
            assertEquals("autor1", stored.getAuthorLogin());
            assertEquals(original.getCreatedAt(), stored.getCreatedAt());
            assertEquals(QuestionStatus.BORRADOR, stored.getStatus());
        }

        @Test
        void otroUsuarioNoPuedeEditarla() {
            Question original = saved("autor1", "Tema", "¿Pregunta original del autor?", QuestionStatus.BORRADOR, 1);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.update(original.getId(), cambios("autor2")));

            assertTrue(ex.getMessage().contains("Solo el autor"));
            assertEquals("¿Pregunta original del autor?", service.findById(original.getId()).getDirectQuestion());
        }

        @Test
        void soloSeEditaEnBorrador() {
            Question enRevision = saved("autor1", "Tema", "¿Pregunta enviada a revision?",
                    QuestionStatus.PENDIENTE_REVISION, 1);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.update(enRevision.getId(), cambios("autor1")));

            assertTrue(ex.getMessage().contains("Borrador"));
        }

        @Test
        void losCambiosPasanPorLaValidacionEstructural() {
            Question original = saved("autor1", "Tema", "¿Pregunta original del autor?", QuestionStatus.BORRADOR, 1);
            QuestionRequest invalida = TestData.withCorrectAnswer(cambios("autor1"), "No es una de las opciones");

            assertThrows(QuestionValidationException.class, () -> service.update(original.getId(), invalida));
            assertEquals("¿Pregunta original del autor?", service.findById(original.getId()).getDirectQuestion());
        }

        @Test
        void editarUnaPreguntaInexistenteResponde404() {
            assertThrows(ResourceNotFoundException.class, () -> service.update("no-existe", cambios("autor1")));
        }

        @Test
        void unaPreguntaDeCasoNoDuplicaElPrefijoDelContextoAlEditarse() {
            Question caso = Question.builder().type("CASO").fromRequest(TestData.validRequest("CASO"))
                    .context("[Analisis de caso] Contexto original").authorLogin("autor1").build();
            repository.save(caso);

            Question updated = service.update(caso.getId(), cambios("autor1"));

            assertEquals("CASO", updated.getType());
            assertEquals("Contexto actualizado por el autor.", updated.getContext());
        }
    }
}
