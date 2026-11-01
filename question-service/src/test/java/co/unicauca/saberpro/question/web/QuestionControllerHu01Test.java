package co.unicauca.saberpro.question.web;

import co.unicauca.saberpro.common.events.DomainEvent;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.web.GlobalApiExceptionHandler;
import co.unicauca.saberpro.question.access.SQLiteQuestionRepository;
import co.unicauca.saberpro.question.application.QuestionApplicationService;
import co.unicauca.saberpro.question.domain.factory.QuestionFactoryRegistry;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU01 (PSP-6) a traves del API REST: la peticion 08 de Postman y la validacion estructural (400). */
class QuestionControllerHu01Test {

    /** Cuerpo de la peticion 08 de la coleccion de Postman. */
    private static final String PETICION_08 = """
            {"type":"MULTIPLE_CHOICE",
             "context":"Una empresa necesita que su banco de preguntas admita nuevos tipos de pregunta sin modificar el nucleo del sistema.",
             "directQuestion":"¿Que patron arquitectonico permite agregar funcionalidad mediante plugins sin modificar el nucleo?",
             "distractors":["Microkernel","Cliente-Servidor","Capas","Broker"],"correctAnswer":"Microkernel",
             "justification":"El patron Microkernel separa un nucleo minimo de los plugins que extienden su funcionalidad.",
             "bibliography":"Richards, M. Software Architecture Patterns. O'Reilly, 2015.",
             "competence":"Arquitectura de software","topic":"Patrones arquitectonicos","subtopic":"Microkernel",
             "difficultyLevel":"MEDIO","authorLogin":"autor1"}""";

    @TempDir
    Path tempDir;

    private SQLiteQuestionRepository repository;
    private MockMvc mvc;
    private final List<DomainEvent> published = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repository = new SQLiteQuestionRepository("jdbc:sqlite:" + tempDir.resolve("api01.db"));
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(published::add);
        mvc = MockMvcBuilders.standaloneSetup(new QuestionController(
                        new QuestionApplicationService(repository, new QuestionFactoryRegistry(), bus)))
                .setControllerAdvice(new GlobalApiExceptionHandler()).build();
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void peticion08CreaLaPreguntaEnBorradorYSePuedeConsultar() throws Exception {
        String body = mvc.perform(post("/api/questions").contentType(MediaType.APPLICATION_JSON).content(PETICION_08))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BORRADOR"))
                .andExpect(jsonPath("$.statusColor").value("#FFE082"))
                .andExpect(jsonPath("$.editable").value(true))
                .andExpect(jsonPath("$.distractors", hasSize(4)))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/questions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctAnswer").value("Microkernel"))
                .andExpect(jsonPath("$.authorLogin").value("autor1"));
        assertEquals(1, published.size());
    }

    @Test
    void sinTipoSeAsumeSeleccionMultiple() throws Exception {
        mvc.perform(post("/api/questions").contentType(MediaType.APPLICATION_JSON)
                        .content(PETICION_08.replace("\"type\":\"MULTIPLE_CHOICE\",", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("MULTIPLE_CHOICE"));
    }

    @Test
    void unaPreguntaInvalidaResponde400ConTodosLosErroresYNoSeGuarda() throws Exception {
        String invalida = PETICION_08
                .replace("\"distractors\":[\"Microkernel\",\"Cliente-Servidor\",\"Capas\",\"Broker\"]",
                        "\"distractors\":[\"Microkernel\",\"Capas\",\"capas\",\"Broker\"]")
                .replace("\"correctAnswer\":\"Microkernel\"", "\"correctAnswer\":\"Hexagonal\"")
                .replace("\"bibliography\":\"Richards, M. Software Architecture Patterns. O'Reilly, 2015.\"",
                        "\"bibliography\":\"\"");

        mvc.perform(post("/api/questions").contentType(MediaType.APPLICATION_JSON).content(invalida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasSize(3)));

        mvc.perform(get("/api/questions?author=autor1")).andExpect(jsonPath("$.totalItems").value(0));
        assertEquals(0, published.size());
    }
}
