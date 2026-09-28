package co.unicauca.saberpro.service;

import co.unicauca.saberpro.domain.*;
import co.unicauca.saberpro.microkernel.core.QuestionMicrokernel;
import co.unicauca.saberpro.microkernel.plugins.MultipleChoiceQuestionPlugin;
import co.unicauca.saberpro.testdoubles.InMemoryQuestionRepositoryFake;
import co.unicauca.saberpro.testdoubles.InMemoryUserRepositoryFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de QuestionService cubriendo las 4 historias de usuario del corte. */
class QuestionServiceTest {

    private QuestionService questionService;
    private UserService userService;
    private InMemoryQuestionRepositoryFake questionRepository;
    private final List<String> sentEmails = new ArrayList<>();

    @BeforeEach
    void setUp() {
        InMemoryUserRepositoryFake userRepository = new InMemoryUserRepositoryFake();
        userService = new UserService(userRepository, new DefaultPasswordPolicy(), new PBKDF2PasswordHasher());
        userService.register("autor1", "Autor Uno", "autor1@x.co", Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "Autor123!");
        userService.register("rev1", "Revisor Uno", "rev1@x.co", Role.REVISOR, UserStatus.ACTIVO, "Revisor123!");

        questionRepository = new InMemoryQuestionRepositoryFake();
        QuestionMicrokernel microkernel = new QuestionMicrokernel();
        IEmailService emailService = (to, name, id, title) -> sentEmails.add(to);

        questionService = new QuestionService(questionRepository, microkernel, userService, emailService);
    }

    private QuestionRequest requestFor(String topic) {
        return new QuestionRequest(MultipleChoiceQuestionPlugin.TYPE,
                "Contexto descriptivo y valido para la pregunta.",
                "¿Cual es la respuesta correcta para el tema " + topic + "?",
                Arrays.asList("A", "B", "C", "D"), "A",
                "Justificacion valida", "Bibliografia valida",
                "Arquitectura de software", topic, "Subtema", DifficultyLevel.MEDIO, "autor1");
    }

    @Test
    void hu01CrearPreguntaValidaQuedaEnBorrador() {
        OperationResult result = questionService.crearPregunta(requestFor("DDD"));
        assertTrue(result.isSuccess());

        List<Question> saved = questionRepository.findAll();
        assertEquals(1, saved.size());
        assertEquals(QuestionState.BORRADOR, saved.get(0).getState());
    }

    @Test
    void hu03CrearPreguntaInvalidaEsRechazadaPorElPipeline() {
        QuestionRequest invalida = new QuestionRequest(MultipleChoiceQuestionPlugin.TYPE,
                "", "corta", Arrays.asList("a"), "", "", "", "", "", "", null, "autor1");
        OperationResult result = questionService.crearPregunta(invalida);

        assertFalse(result.isSuccess());
        assertTrue(questionRepository.findAll().isEmpty());
    }

    @Test
    void hu02SoloElAutorPuedeEnviarSuPreguntaARevision() {
        questionService.crearPregunta(requestFor("DDD"));
        String id = questionRepository.findAll().get(0).getId();

        OperationResult otroAutor = questionService.marcarPendienteDeRevision(id, "intruso");
        assertFalse(otroAutor.isSuccess());

        OperationResult correcto = questionService.marcarPendienteDeRevision(id, "autor1");
        assertTrue(correcto.isSuccess());
        assertEquals(QuestionState.PENDIENTE_REVISION, questionRepository.findById(id).get().getState());
    }

    @Test
    void hu02NoSePuedeReenviarUnaPreguntaQueYaNoEstaEnBorrador() {
        questionService.crearPregunta(requestFor("DDD"));
        String id = questionRepository.findAll().get(0).getId();
        questionService.marcarPendienteDeRevision(id, "autor1");

        OperationResult segundoIntento = questionService.marcarPendienteDeRevision(id, "autor1");
        assertFalse(segundoIntento.isSuccess());
    }

    @Test
    void hu03ListarMisPreguntasAplicaPaginacionYFiltros() {
        for (int i = 0; i < 5; i++) {
            questionService.crearPregunta(requestFor("Tema" + i));
        }

        QuestionFilterCriteria criteria = new QuestionFilterCriteria()
                .setAuthorLogin("autor1").setPage(1).setPageSize(2);
        PagedResult<Question> page1 = questionService.listarMisPreguntas(criteria);

        assertEquals(2, page1.getItems().size());
        assertEquals(5, page1.getTotalItems());
        assertEquals(3, page1.getTotalPages());

        criteria.setTopic("Tema3");
        PagedResult<Question> filtered = questionService.listarMisPreguntas(criteria);
        assertEquals(1, filtered.getTotalItems());
    }

    @Test
    void hu04AsignarRevisorCambiaEstadoYEnviaCorreo() {
        questionService.crearPregunta(requestFor("DDD"));
        String id = questionRepository.findAll().get(0).getId();
        questionService.marcarPendienteDeRevision(id, "autor1");

        OperationResult result = questionService.asignarRevisores(id, List.of("rev1"));

        assertTrue(result.isSuccess());
        Question updated = questionRepository.findById(id).get();
        assertEquals(QuestionState.EN_REVISION, updated.getState());
        assertTrue(updated.getAssignedReviewers().contains("rev1"));
        assertEquals(1, sentEmails.size());
        assertEquals("rev1@x.co", sentEmails.get(0));
    }

    @Test
    void hu04NoSePuedeAsignarRevisorAUnaPreguntaQueNoEstaPendiente() {
        questionService.crearPregunta(requestFor("DDD")); // queda en BORRADOR
        String id = questionRepository.findAll().get(0).getId();

        OperationResult result = questionService.asignarRevisores(id, List.of("rev1"));
        assertFalse(result.isSuccess());
    }

    @Test
    void hu04ExigeAlMenosUnRevisorSeleccionado() {
        questionService.crearPregunta(requestFor("DDD"));
        String id = questionRepository.findAll().get(0).getId();
        questionService.marcarPendienteDeRevision(id, "autor1");

        OperationResult result = questionService.asignarRevisores(id, List.of());
        assertFalse(result.isSuccess());
    }

    // ------------------------------------------------------------------
    // Operaciones agregadas para la API REST (Taller de Microservicios)
    // ------------------------------------------------------------------

    @Test
    void apiCrearPreguntaDevuelveLaPreguntaCreada() {
        OperationResult result = questionService.crearPregunta(requestFor("DDD"));

        assertTrue(result.isSuccess());
        Question creada = (Question) result.getData();
        assertNotNull(creada);
        assertEquals(creada.getId(), questionRepository.findAll().get(0).getId());
    }

    @Test
    void apiActualizarPreguntaConservaIdYEstado() {
        Question creada = (Question) questionService.crearPregunta(requestFor("DDD")).getData();

        OperationResult result = questionService.actualizarPregunta(creada.getId(), requestFor("Microservicios"));

        assertTrue(result.isSuccess());
        Question editada = questionRepository.findById(creada.getId()).get();
        assertEquals("Microservicios", editada.getTopic());
        assertEquals(QuestionState.BORRADOR, editada.getState());
        assertEquals(1, questionRepository.findAll().size());
    }

    @Test
    void apiActualizarPreguntaInvalidaEsRechazadaPorElPipeline() {
        Question creada = (Question) questionService.crearPregunta(requestFor("DDD")).getData();
        QuestionRequest invalida = new QuestionRequest(MultipleChoiceQuestionPlugin.TYPE,
                "", "corta", Arrays.asList("a"), "", "", "", "", "", "", null, "autor1");

        OperationResult result = questionService.actualizarPregunta(creada.getId(), invalida);

        assertFalse(result.isSuccess());
        assertEquals("DDD", questionRepository.findById(creada.getId()).get().getTopic());
    }

    @Test
    void apiSoloSeEditanPreguntasEnBorradorYDelMismoAutor() {
        Question creada = (Question) questionService.crearPregunta(requestFor("DDD")).getData();
        QuestionRequest deOtroAutor = new QuestionRequest(MultipleChoiceQuestionPlugin.TYPE,
                "Contexto descriptivo y valido para la pregunta.", "¿Pregunta valida de otro autor?",
                Arrays.asList("A", "B", "C", "D"), "A", "Justificacion", "Bibliografia",
                "Arquitectura de software", "DDD", "Subtema", DifficultyLevel.MEDIO, "intruso");
        assertFalse(questionService.actualizarPregunta(creada.getId(), deOtroAutor).isSuccess());

        questionService.marcarPendienteDeRevision(creada.getId(), "autor1");
        assertFalse(questionService.actualizarPregunta(creada.getId(), requestFor("Otro")).isSuccess());
    }

    @Test
    void apiEliminarPreguntaEsLogica() {
        Question creada = (Question) questionService.crearPregunta(requestFor("DDD")).getData();

        OperationResult result = questionService.eliminarPregunta(creada.getId());

        assertTrue(result.isSuccess());
        assertEquals(QuestionState.ELIMINADA, questionRepository.findById(creada.getId()).get().getState());
        assertFalse(questionService.eliminarPregunta(creada.getId()).isSuccess());
        assertFalse(questionService.eliminarPregunta("no-existe").isSuccess());
    }

    @Test
    void hu04ListarPendientesSoloDevuelvePreguntasPendientesDeRevision() {
        Question enviada = (Question) questionService.crearPregunta(requestFor("DDD")).getData();
        questionService.crearPregunta(requestFor("Otro")); // queda en Borrador
        questionService.marcarPendienteDeRevision(enviada.getId(), "autor1");

        List<Question> pendientes = questionService.listarPendientesDeRevision();

        assertEquals(1, pendientes.size());
        assertEquals(enviada.getId(), pendientes.get(0).getId());
    }

    @Test
    void hu03BuscarPorIdDevuelveLaPreguntaParaVerSuDetalle() {
        Question creada = (Question) questionService.crearPregunta(requestFor("DDD")).getData();

        assertTrue(questionService.buscarPorId(creada.getId()).isPresent());
        assertTrue(questionService.buscarPorId("no-existe").isEmpty());
    }
}
