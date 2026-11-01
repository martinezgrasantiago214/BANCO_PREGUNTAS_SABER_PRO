package co.unicauca.saberpro.notification.application;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.events.ReviewersAssignedEvent;
import co.unicauca.saberpro.common.web.PendingIssueException;
import co.unicauca.saberpro.notification.channel.NotificationChannel;
import co.unicauca.saberpro.notification.directory.UserDirectory;
import co.unicauca.saberpro.notification.domain.DeliveryResult;
import co.unicauca.saberpro.notification.domain.Notification;
import co.unicauca.saberpro.notification.domain.Recipient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {

    private final Notification notification = new Notification("a@x.co", "Autor", "Asunto", "Cuerpo");

    private static final UserDirectory NO_USERS = new UserDirectory() {
        @Override
        public Optional<Recipient> findByLogin(String login) {
            return Optional.empty();
        }

        @Override
        public List<Recipient> findAdministrators() {
            return List.of();
        }
    };

    /** Canal de prueba que registra lo que recibe y responde lo indicado. */
    private static final class RecordingChannel implements NotificationChannel {
        private final String name;
        private final boolean succeeds;
        final List<Notification> sent = new ArrayList<>();

        RecordingChannel(String name, boolean succeeds) {
            this.name = name;
            this.succeeds = succeeds;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public DeliveryResult send(Notification n) {
            sent.add(n);
            return succeeds ? DeliveryResult.delivered(name, "ok") : DeliveryResult.failed(name, "caido");
        }
    }

    @Test
    void siElCanalPrincipalFuncionaNoSeUsaElDeRespaldo() {
        RecordingChannel smtp = new RecordingChannel("SMTP", true);
        RecordingChannel outbox = new RecordingChannel("OUTBOX", true);

        DeliveryResult result = new NotificationService(smtp, outbox, NO_USERS).deliver(notification);

        assertTrue(result.delivered());
        assertEquals("SMTP", result.channel());
        assertTrue(outbox.sent.isEmpty());
    }

    @Test
    void siElCanalPrincipalFallaSeUsaElDeRespaldoYSeExplicaPorQue() {
        RecordingChannel smtp = new RecordingChannel("SMTP", false);
        RecordingChannel outbox = new RecordingChannel("OUTBOX", true);

        DeliveryResult result = new NotificationService(smtp, outbox, NO_USERS).deliver(notification);

        assertTrue(result.delivered());
        assertEquals("OUTBOX", result.channel());
        assertTrue(result.detail().contains("SMTP: caido"));
        assertEquals(1, outbox.sent.size());
    }

    @Test
    void losManejadoresDeEventosPendientesIndicanSuIssue() {
        NotificationService service = new NotificationService(new RecordingChannel("SMTP", true), null, NO_USERS);

        PendingIssueException assigned = assertThrows(PendingIssueException.class, () -> service.onReviewersAssigned(
                new ReviewersAssignedEvent("q-1", "¿P?", List.of("rev1"), "admin", Instant.now())));
        assertEquals("PSP-34", assigned.getIssueKey());
    }

    // ---- HU05 (PSP-49 y PSP-50): aviso de la evaluacion ----

    private static final Recipient AUTOR = new Recipient("autor1", "Autor Uno", "autor1@x.co", "AUTOR_PREGUNTAS");
    private static final Recipient ADMIN = new Recipient("admin", "Admin", "admin@x.co", "ADMINISTRADOR");
    private static final Recipient ADMIN2 = new Recipient("admin2", "Admin Dos", "admin2@x.co", "ADMINISTRADOR");

    /** Directorio de prueba con los usuarios indicados. */
    private static UserDirectory directory(List<Recipient> all) {
        return new UserDirectory() {
            @Override
            public Optional<Recipient> findByLogin(String login) {
                return all.stream().filter(r -> r.login().equals(login)).findFirst();
            }

            @Override
            public List<Recipient> findAdministrators() {
                return all.stream().filter(r -> "ADMINISTRADOR".equals(r.role())).toList();
            }
        };
    }

    private static QuestionEvaluatedEvent rejected(String authorLogin) {
        return new QuestionEvaluatedEvent("q-1", authorLogin, "¿Que patron permite agregar plugins?", "rev1",
                EvaluationVerdict.RECHAZADA, "El distractor C es demasiado obvio.", Instant.now());
    }

    @Test
    void laEvaluacionSeAvisaAlAutorYACadaAdministrador() {
        RecordingChannel smtp = new RecordingChannel("SMTP", true);
        NotificationService service = new NotificationService(smtp, null, directory(List.of(AUTOR, ADMIN, ADMIN2)));

        List<DeliveryResult> results = service.onQuestionEvaluated(rejected("autor1"));

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(DeliveryResult::delivered));
        assertEquals(List.of("autor1@x.co", "admin@x.co", "admin2@x.co"),
                smtp.sent.stream().map(Notification::recipientEmail).toList());

        Notification toAuthor = smtp.sent.get(0);
        assertTrue(toAuthor.subject().contains("Rechazada"));
        assertTrue(toAuthor.body().contains("Su pregunta fue evaluada"));
        assertTrue(toAuthor.body().contains("¿Que patron permite agregar plugins?"));
        assertTrue(toAuthor.body().contains("El distractor C es demasiado obvio."));
        assertTrue(toAuthor.body().contains("rev1"));

        Notification toAdmin = smtp.sent.get(1);
        assertTrue(toAdmin.body().contains("Autor: autor1"));
    }

    @Test
    void siElAutorTambienEsAdministradorRecibeUnSoloCorreo() {
        Recipient autorAdmin = new Recipient("autor1", "Autor Admin", "autor1@x.co", "ADMINISTRADOR");
        RecordingChannel smtp = new RecordingChannel("SMTP", true);
        NotificationService service = new NotificationService(smtp, null, directory(List.of(autorAdmin)));

        service.onQuestionEvaluated(rejected("autor1"));

        assertEquals(1, smtp.sent.size());
        assertTrue(smtp.sent.get(0).body().contains("Su pregunta fue evaluada"));
    }

    @Test
    void siNoSeEncuentraAlAutorIgualSeAvisaALosAdministradores() {
        RecordingChannel smtp = new RecordingChannel("SMTP", true);
        NotificationService service = new NotificationService(smtp, null, directory(List.of(ADMIN)));

        List<DeliveryResult> results = service.onQuestionEvaluated(rejected("autor-borrado"));

        assertEquals(1, results.size());
        assertEquals("admin@x.co", smtp.sent.get(0).recipientEmail());
    }

    @Test
    void siUnEnvioFallaSeIntentaIgualConLosDemas() {
        NotificationChannel flaky = new NotificationChannel() {
            @Override
            public String name() {
                return "SMTP";
            }

            @Override
            public DeliveryResult send(Notification n) {
                if (n.recipientEmail().equals("autor1@x.co")) {
                    throw new IllegalStateException("buzon lleno");
                }
                return DeliveryResult.delivered("SMTP", "ok");
            }
        };
        NotificationService service = new NotificationService(flaky, null, directory(List.of(AUTOR, ADMIN)));

        List<DeliveryResult> results = service.onQuestionEvaluated(rejected("autor1"));

        assertEquals(2, results.size());
        assertFalse(results.get(0).delivered());
        assertTrue(results.get(1).delivered());
    }

    @Test
    void siElSmtpFallaElAvisoQuedaEnLaBandejaDeRespaldo() {
        RecordingChannel smtp = new RecordingChannel("SMTP", false);
        RecordingChannel outbox = new RecordingChannel("OUTBOX", true);
        NotificationService service = new NotificationService(smtp, outbox, directory(List.of(AUTOR)));

        List<DeliveryResult> results = service.onQuestionEvaluated(rejected("autor1"));

        assertEquals("OUTBOX", results.get(0).channel());
        assertEquals(1, outbox.sent.size());
    }
}
