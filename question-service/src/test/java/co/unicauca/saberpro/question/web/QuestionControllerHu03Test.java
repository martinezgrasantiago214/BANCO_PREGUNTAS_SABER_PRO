package co.unicauca.saberpro.question.web;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.web.GlobalApiExceptionHandler;
import co.unicauca.saberpro.question.TestData;
import co.unicauca.saberpro.question.access.SQLiteQuestionRepository;
import co.unicauca.saberpro.question.application.QuestionApplicationService;
import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.factory.QuestionFactoryRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU03 (PSP-9) a traves del API REST: el listado (peticion 09 de Postman) y
 * la edicion (peticion 11) con sus codigos HTTP: 200, 403, 409, 400 y 404.
 */
class QuestionControllerHu03Test {

    @TempDir
    Path tempDir;

    private SQLiteQuestionRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = new SQLiteQuestionRepository("jdbc:sqlite:" + tempDir.resolve("api.db"));
        QuestionApplicationService service = new QuestionApplicationService(repository,
                new QuestionFactoryRegistry(), new DomainEventBus());
        mvc = MockMvcBuilders.standaloneSetup(new QuestionController(service))
                .setControllerAdvice(new GlobalApiExceptionHandler()).build();
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    private Question saved(String author, QuestionStatus status) {
        Question q = Question.builder().type("MULTIPLE_CHOICE").fromRequest(TestData.validRequest("MULTIPLE_CHOICE"))
                .authorLogin(author).status(status).build();
        repository.save(q);
        return q;
    }

    /** Cuerpo de la peticion 11 de Postman, con el autor indicado. */
    private static String edicion(String author) {
        return """
                {"type":"MULTIPLE_CHOICE","context":"Contexto editado de la pregunta.",
                 "directQuestion":"¿Que patron permite agregar plugins sin tocar el nucleo?",
                 "distractors":["Microkernel","Cliente-Servidor","Capas","Broker"],"correctAnswer":"Microkernel",
                 "justification":"El Microkernel separa el nucleo de los plugins.",
                 "bibliography":"Richards, M. Software Architecture Patterns. O'Reilly, 2015.",
                 "competence":"Arquitectura de software","topic":"Patrones arquitectonicos","subtopic":"Microkernel",
                 "difficultyLevel":"ALTO","authorLogin":"%s"}""".formatted(author);
    }

    @Test
    void peticion09ListaLasPreguntasDelAutorConFiltrosYPaginacion() throws Exception {
        saved("autor1", QuestionStatus.BORRADOR);
        saved("autor1", QuestionStatus.APROBADA);
        saved("autor2", QuestionStatus.BORRADOR);

        mvc.perform(get("/api/questions?author=autor1&status=BORRADOR&keyword=patron&page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].authorLogin").value("autor1"))
                .andExpect(jsonPath("$.items[0].statusColor").value("#FFE082"));
    }

    @Test
    void peticion11EditaElBorradorDelAutor() throws Exception {
        Question q = saved("autor1", QuestionStatus.BORRADOR);

        mvc.perform(put("/api/questions/" + q.getId()).contentType(MediaType.APPLICATION_JSON).content(edicion("autor1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directQuestion").value("¿Que patron permite agregar plugins sin tocar el nucleo?"))
                .andExpect(jsonPath("$.status").value("BORRADOR"));
    }

    @Test
    void editarUnaPreguntaAjenaResponde403() throws Exception {
        Question q = saved("autor1", QuestionStatus.BORRADOR);

        mvc.perform(put("/api/questions/" + q.getId()).contentType(MediaType.APPLICATION_JSON).content(edicion("autor2")))
                .andExpect(status().isForbidden());
    }

    @Test
    void editarUnaPreguntaQueNoEstaEnBorradorResponde409() throws Exception {
        Question q = saved("autor1", QuestionStatus.PENDIENTE_REVISION);

        mvc.perform(put("/api/questions/" + q.getId()).contentType(MediaType.APPLICATION_JSON).content(edicion("autor1")))
                .andExpect(status().isConflict());
    }

    @Test
    void unaEdicionInvalidaResponde400YUnaInexistente404() throws Exception {
        Question q = saved("autor1", QuestionStatus.BORRADOR);

        mvc.perform(put("/api/questions/" + q.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(edicion("autor1").replace("\"correctAnswer\":\"Microkernel\"", "\"correctAnswer\":\"Otra\"")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/questions/no-existe").contentType(MediaType.APPLICATION_JSON).content(edicion("autor1")))
                .andExpect(status().isNotFound());
    }
}
