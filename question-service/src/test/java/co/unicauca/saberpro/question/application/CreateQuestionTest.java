package co.unicauca.saberpro.question.application;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.events.DomainEvent;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.EventTopics;
import co.unicauca.saberpro.common.events.QuestionCreatedEvent;
import co.unicauca.saberpro.common.web.BusinessException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU01 (PSP-6): crear preguntas de seleccion multiple con unica respuesta.
 * PSP-19: una pregunta valida se guarda en Borrador y publica
 * question.created; cada regla de la validacion estructural que falla
 * impide guardarla y se reporta.
 */
class CreateQuestionTest {

    private static final String MC = "MULTIPLE_CHOICE";

    @TempDir
    Path tempDir;

    private SQLiteQuestionRepository repository;
    private QuestionApplicationService service;
    private final List<DomainEvent> published = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repository = new SQLiteQuestionRepository("jdbc:sqlite:" + tempDir.resolve("hu01.db"));
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(published::add);
        service = new QuestionApplicationService(repository, new QuestionFactoryRegistry(), bus);
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    /** Solicitud valida; cada prueba cambia solo lo que necesita. */
    private static Req valida() {
        return new Req();
    }

    /** Constructor de solicitudes para las pruebas (todos los campos validos por defecto). */
    private static final class Req {
        String type = MC;
        String context = "Una empresa necesita escalar el envio de notificaciones en las convocatorias.";
        String directQuestion = "¿Que estilo arquitectonico favorece la escalabilidad independiente?";
        List<String> distractors = Arrays.asList("Monolito en capas", "Microservicios con eventos",
                "Cliente-servidor", "Tuberias y filtros");
        String correctAnswer = "Microservicios con eventos";
        String justification = "Los microservicios permiten escalar cada componente por separado.";
        String bibliography = "Richards, M. Fundamentals of Software Architecture, 2020.";
        String competence = "Diseno de software";
        String topic = "Arquitectura";
        String subtopic = "Estilos arquitectonicos";
        DifficultyLevel difficulty = DifficultyLevel.MEDIO;
        String author = "autor1";

        QuestionRequest build() {
            return new QuestionRequest(type, context, directQuestion, distractors, correctAnswer, justification,
                    bibliography, competence, topic, subtopic, difficulty, author);
        }
    }

    private List<String> erroresDe(Req req) {
        QuestionValidationException ex = assertThrows(QuestionValidationException.class,
                () -> service.create(req.build()));
        return ex.getMessages();
    }

    private void assertNadaGuardadoNiPublicado() {
        assertEquals(0, repository.search(new co.unicauca.saberpro.question.access.QuestionFilterCriteria())
                .totalItems());
        assertTrue(published.isEmpty());
    }

    @Nested
    class PreguntaValida {

        @Test
        void seGuardaEnBorradorConTodosSusCampos() {
            Question created = service.create(valida().build());

            Question stored = service.findById(created.getId());
            assertEquals(QuestionStatus.BORRADOR, stored.getStatus());
            assertEquals(MC, stored.getType());
            assertEquals("autor1", stored.getAuthorLogin());
            assertEquals(4, stored.getDistractors().size());
            assertEquals("Microservicios con eventos", stored.getCorrectAnswer());
            assertEquals("Diseno de software", stored.getCompetence());
            assertEquals("Arquitectura", stored.getTopic());
            assertEquals("Estilos arquitectonicos", stored.getSubtopic());
            assertEquals(DifficultyLevel.MEDIO, stored.getDifficultyLevel());
            assertTrue(stored.isEditable());
        }

        @Test
        void publicaQuestionCreated() {
            Question created = service.create(valida().build());

            assertEquals(1, published.size());
            QuestionCreatedEvent event = (QuestionCreatedEvent) published.get(0);
            assertEquals(EventTopics.QUESTION_CREATED, event.routingKey());
            assertEquals(created.getId(), event.questionId());
            assertEquals("autor1", event.authorLogin());
            assertEquals("Arquitectura", event.topic());
        }

        @Test
        void normalizaEspaciosYEscribeLaRespuestaCorrectaIgualQueSuDistractor() {
            Req req = valida();
            req.distractors = Arrays.asList("  Monolito en capas ", "Microservicios con eventos  ",
                    "Cliente-servidor", "Tuberias y filtros");
            req.correctAnswer = "  microservicios CON eventos ";
            req.topic = "  Arquitectura  ";

            Question created = service.create(req.build());

            assertEquals("Monolito en capas", created.getDistractors().get(0));
            assertEquals("Microservicios con eventos", created.getCorrectAnswer());
            assertTrue(created.getDistractors().contains(created.getCorrectAnswer()));
            assertEquals("Arquitectura", created.getTopic());
        }

        @Test
        void cadaPreguntaRecibeSuPropioId() {
            Question a = service.create(valida().build());
            Question b = service.create(valida().build());

            assertFalse(a.getId().equals(b.getId()));
        }
    }

    /** PSP-16: cuatro distractores y una respuesta correcta entre ellos. */
    @Nested
    class DistractoresYRespuestaCorrecta {

        @Test
        void exigeExactamenteCuatroDistractores() {
            Req tres = valida();
            tres.distractors = List.of("A", "Microservicios con eventos", "C");
            Req cinco = valida();
            cinco.distractors = List.of("A", "Microservicios con eventos", "C", "D", "E");

            assertTrue(erroresDe(tres).get(0).contains("exactamente 4 distractores (se recibieron 3)"));
            assertTrue(erroresDe(cinco).get(0).contains("se recibieron 5"));
            assertNadaGuardadoNiPublicado();
        }

        @Test
        void unDistractorNoPuedeEstarVacio() {
            Req req = valida();
            req.distractors = Arrays.asList("Monolito en capas", "Microservicios con eventos", "   ", "D");

            assertTrue(erroresDe(req).contains("El distractor C esta vacio."));
            assertNadaGuardadoNiPublicado();
        }

        @Test
        void losDistractoresNoSePuedenRepetirAunqueCambienMayusculasOEspacios() {
            Req req = valida();
            req.distractors = Arrays.asList("Monolito en capas", "Microservicios con eventos",
                    " monolito EN capas ", "D");

            assertTrue(erroresDe(req).get(0).contains("no se pueden repetir"));
            assertNadaGuardadoNiPublicado();
        }

        @Test
        void laRespuestaCorrectaDebeSerUnoDeLosDistractores() {
            Req req = valida();
            req.correctAnswer = "Arquitectura hexagonal";

            assertTrue(erroresDe(req).get(0).contains("respuesta correcta"));
            assertNadaGuardadoNiPublicado();
        }

        @Test
        void laRespuestaCorrectaEsObligatoria() {
            Req req = valida();
            req.correctAnswer = " ";

            assertTrue(erroresDe(req).get(0).contains("respuesta correcta"));
        }
    }

    /** PSP-17: una prueba por cada regla de la validacion estructural. */
    @Nested
    class ValidacionEstructural {

        @Test
        void contextoObligatorio() {
            Req req = valida();
            req.context = "";
            assertTrue(erroresDe(req).get(0).contains("contexto"));
        }

        @Test
        void preguntaDirectaConMinimoDeCaracteres() {
            Req req = valida();
            req.directQuestion = "¿Corta?";
            assertTrue(erroresDe(req).get(0).contains("pregunta directa"));
        }

        @Test
        void clasificacionCompleta() {
            for (String campo : List.of("competencia", "tema", "subtema", "dificultad")) {
                Req req = valida();
                switch (campo) {
                    case "competencia" -> req.competence = null;
                    case "tema" -> req.topic = " ";
                    case "subtema" -> req.subtopic = "";
                    default -> req.difficulty = null;
                }
                assertTrue(erroresDe(req).get(0).contains("Competencia, Tema, Subtema y Nivel de dificultad"),
                        "Falta " + campo);
            }
            assertNadaGuardadoNiPublicado();
        }

        @Test
        void justificacionBibliografiaYAutorObligatorios() {
            for (String campo : List.of("justificacion", "bibliografia", "autor")) {
                Req req = valida();
                switch (campo) {
                    case "justificacion" -> req.justification = "";
                    case "bibliografia" -> req.bibliography = null;
                    default -> req.author = " ";
                }
                assertTrue(erroresDe(req).get(0).contains("Justificacion"), "Falta " + campo);
            }
            assertNadaGuardadoNiPublicado();
        }

        @Test
        void reportaTodosLosErroresJuntos() {
            Req req = valida();
            req.context = "";
            req.distractors = List.of("A", "B");
            req.correctAnswer = "Z";
            req.topic = null;
            req.bibliography = "";

            List<String> errors = erroresDe(req);

            assertEquals(5, errors.size(), errors.toString());
            assertNadaGuardadoNiPublicado();
        }

        @Test
        void tipoDePreguntaNoSoportado() {
            Req req = valida();
            req.type = "VERDADERO_FALSO";

            BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req.build()));
            assertTrue(ex.getMessage().contains("no soportado"));
            assertNadaGuardadoNiPublicado();
        }
    }
}
