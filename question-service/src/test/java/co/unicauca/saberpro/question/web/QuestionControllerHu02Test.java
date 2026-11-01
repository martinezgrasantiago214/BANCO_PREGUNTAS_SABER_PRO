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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU02 (PSP-8) a traves del API REST: la peticion 12 de Postman y sus codigos 200, 403, 409 y 404. */
class QuestionControllerHu02Test {

    @TempDir
    Path tempDir;

    private SQLiteQuestionRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = new SQLiteQuestionRepository("jdbc:sqlite:" + tempDir.resolve("api02.db"));
        mvc = MockMvcBuilders.standaloneSetup(new QuestionController(
                        new QuestionApplicationService(repository, new QuestionFactoryRegistry(), new DomainEventBus())))
                .setControllerAdvice(new GlobalApiExceptionHandler()).build();
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    private Question guardadaEn(QuestionStatus status) {
        Question q = Question.builder().type("MULTIPLE_CHOICE").fromRequest(TestData.validRequest("MULTIPLE_CHOICE"))
                .status(status).build();
        repository.save(q);
        return q;
    }

    private org.springframework.test.web.servlet.ResultActions enviar(String id, String author) throws Exception {
        return mvc.perform(patch("/api/questions/" + id + "/submit-review")
                .contentType(MediaType.APPLICATION_JSON).content("{\"authorLogin\":\"" + author + "\"}"));
    }

    @Test
    void peticion12PasaElBorradorAPendienteDeRevisionConSuColor() throws Exception {
        Question q = guardadaEn(QuestionStatus.BORRADOR);

        enviar(q.getId(), "autor1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.statusLabel").value("Pendiente de revision"))
                .andExpect(jsonPath("$.statusColor").value("#90CAF9"))
                .andExpect(jsonPath("$.editable").value(false));
    }

    @Test
    void otroAutorResponde403() throws Exception {
        enviar(guardadaEn(QuestionStatus.BORRADOR).getId(), "autor2").andExpect(status().isForbidden());
    }

    @Test
    void unaPreguntaQueNoEstaEnBorradorResponde409() throws Exception {
        enviar(guardadaEn(QuestionStatus.APROBADA).getId(), "autor1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messages[0]").value("No se puede enviar a revision una pregunta en estado Aprobada."));
    }

    @Test
    void unaPreguntaInexistenteResponde404() throws Exception {
        enviar("no-existe", "autor1").andExpect(status().isNotFound());
    }
}
