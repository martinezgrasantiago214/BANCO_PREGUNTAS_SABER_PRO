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

        PendingIssueException evaluated = assertThrows(PendingIssueException.class, () -> service.onQuestionEvaluated(
                new QuestionEvaluatedEvent("q-1", "autor1", "rev1", EvaluationVerdict.APROBADA, "Bien", Instant.now())));
        assertEquals("PSP-49", evaluated.getIssueKey());
    }
}
