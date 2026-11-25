package co.unicauca.saberpro.review.infrastructure.adapter.in.web;

import co.unicauca.saberpro.common.web.GlobalApiExceptionHandler;
import co.unicauca.saberpro.review.application.service.AssignReviewersService;
import co.unicauca.saberpro.review.application.service.EvaluateQuestionService;
import co.unicauca.saberpro.review.application.service.RegisterSubmissionService;
import co.unicauca.saberpro.review.application.service.ReviewQueryService;
import co.unicauca.saberpro.review.testdoubles.InMemoryQuestionReviewRepository;
import co.unicauca.saberpro.review.testdoubles.RecordingEventPublisher;
import co.unicauca.saberpro.review.testdoubles.ReviewFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;

import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.QUESTION;
import static co.unicauca.saberpro.review.testdoubles.ReviewFixtures.submission;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Adaptador REST del review-service: codigos HTTP y JSON de HU04, HU05 y
 * HU06. Hasta #49 la identidad viaja en la peticion (assignedBy,
 * reviewerLogin, requester), como en el segundo corte.
 */
class ReviewControllerTest {

    private MockMvc mvc;
    private RegisterSubmissionService register;

    @BeforeEach
    void setUp() {
        InMemoryQuestionReviewRepository repository = new InMemoryQuestionReviewRepository();
        RecordingEventPublisher publisher = new RecordingEventPublisher();
        Clock clock = Clock.systemUTC();
        register = new RegisterSubmissionService(repository, publisher);
        ReviewController controller = new ReviewController(
                new AssignReviewersService(repository, ReviewFixtures.directory(), publisher, clock),
                new EvaluateQuestionService(repository, publisher, clock),
                new ReviewQueryService(repository));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ReviewApiExceptionHandler(), new GlobalApiExceptionHandler())
                .build();
        register.register(submission(1));
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void assignRev1() throws Exception {
        postJson("/api/reviews/assignments",
                "{\"questionId\":\"" + QUESTION + "\",\"reviewers\":[\"rev1\"],\"assignedBy\":\"admin\"}")
                .andExpect(status().isCreated());
    }

    private ResultActions evaluate(String reviewer, String verdict, String observations) throws Exception {
        return postJson("/api/reviews/evaluations", "{\"questionId\":\"" + QUESTION + "\",\"reviewerLogin\":\""
                + reviewer + "\",\"verdict\":\"" + verdict + "\",\"observations\":\"" + observations + "\"}");
    }

    @Test
    void hu04ListaLasPendientesYAsignaRevisores() throws Exception {
        mvc.perform(get("/api/reviews/pending-questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDIENTE_ASIGNACION"));

        postJson("/api/reviews/assignments",
                "{\"questionId\":\"" + QUESTION + "\",\"reviewers\":[\"rev1\",\"rev2\"],\"assignedBy\":\"admin\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].assignedBy").value("admin"))
                .andExpect(jsonPath("$[0].round").value(1))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void hu04ReglasIncumplidasResponden400YPreguntaInexistente404() throws Exception {
        postJson("/api/reviews/assignments",
                "{\"questionId\":\"" + QUESTION + "\",\"reviewers\":[\"autor1\"],\"assignedBy\":\"admin\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value(containsString("autor")));
        postJson("/api/reviews/assignments", "{\"questionId\":\"nada\",\"reviewers\":[\"rev1\"],\"assignedBy\":\"admin\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void hu05ElRevisorEvaluaYUnaSegundaEvaluacionResponde409() throws Exception {
        assignRev1();
        evaluate("rev1", "RECHAZADA", "Ajustar")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewerLogin").value("rev1"))
                .andExpect(jsonPath("$.round").value(1));
        evaluate("rev1", "APROBADA", "").andExpect(status().isConflict());
    }

    @Test
    void hu05UnRevisorNoAsignadoRecibe403() throws Exception {
        assignRev1();
        evaluate("rev2", "APROBADA", "").andExpect(status().isForbidden());
        mvc.perform(get("/api/reviews/assignments").param("reviewer", "rev1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].active").value(true));
        mvc.perform(get("/api/reviews/assignments").param("reviewer", "rev2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void hu05RechazarSinObservacionesResponde400() throws Exception {
        assignRev1();
        evaluate("rev1", "RECHAZADA", "").andExpect(status().isBadRequest());
    }

    @Test
    void hu06HistorialConLasDosRondasYAccesoRestringidoPorRequester() throws Exception {
        assignRev1();
        evaluate("rev1", "RECHAZADA", "Citar fuente");
        register.register(submission(2));

        mvc.perform(get("/api/reviews/history/" + QUESTION).param("requester", "autor1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRound").value(2))
                .andExpect(jsonPath("$.currentStatus").value("PENDIENTE_ASIGNACION"))
                .andExpect(jsonPath("$.rounds", hasSize(2)))
                .andExpect(jsonPath("$.rounds[0].evaluation.observations").value("Citar fuente"))
                .andExpect(jsonPath("$.rounds[0].reviewers[0].reviewerLogin").value("rev1"));
        mvc.perform(get("/api/reviews/evaluations").param("questionId", QUESTION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(get("/api/reviews/history/" + QUESTION).param("requester", "rev3"))
                .andExpect(status().isForbidden());
    }
}
