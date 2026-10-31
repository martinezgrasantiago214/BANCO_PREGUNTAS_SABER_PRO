package co.unicauca.saberpro.notifications.application;

import co.unicauca.saberpro.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.events.ReviewersAssignedEvent;
import co.unicauca.saberpro.notifications.application.template.QuestionSubmittedTemplate;
import co.unicauca.saberpro.notifications.application.template.ReviewersAssignedTemplate;
import co.unicauca.saberpro.notifications.domain.DeliveryStatus;
import co.unicauca.saberpro.notifications.domain.Notification;
import co.unicauca.saberpro.notifications.testdoubles.FakeUserDirectory;
import co.unicauca.saberpro.notifications.testdoubles.InMemoryNotificationRepository;
import co.unicauca.saberpro.notifications.testdoubles.RecordingEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {

    private RecordingEmailSender sender;
    private InMemoryNotificationRepository repository;
    private NotificationService service;
    private final ReviewersAssignedEvent assigned = new ReviewersAssignedEvent("evt-1", Instant.now(), "q1", "autor1",
            "¿Qué estilo favorece la escalabilidad?", List.of("rev1", "rev2"), "admin");

    @BeforeEach
    void setUp() {
        sender = new RecordingEmailSender();
        repository = new InMemoryNotificationRepository();
        service = new NotificationService(List.of(new ReviewersAssignedTemplate()), new FakeUserDirectory(),
                sender, repository);
    }

    @Test
    void hu04SendsEmailToEachReviewerAndStoresIt() {
        List<Notification> sent = service.handle(assigned);
        assertEquals(3, sent.size());
        assertEquals(3, sender.sent.size());
        assertEquals("rev1@x.co", sender.sent.get(0).to());
        assertTrue(sent.stream().allMatch(n -> n.getDeliveryStatus() == DeliveryStatus.SIMULADA));
    }

    @Test
    void redeliveredEventDoesNotNotifyTwice() {
        service.handle(assigned);
        assertTrue(service.handle(assigned).isEmpty());
        assertEquals(3, sender.sent.size());
    }

    @Test
    void emailFailureIsRecordedAsFailed() {
        sender.fail = true;
        List<Notification> sent = service.handle(assigned);
        assertTrue(sent.stream().allMatch(n -> n.getDeliveryStatus() == DeliveryStatus.FALLIDA));
    }

    @Test
    void eventsWithoutTemplateAreIgnored() {
        var other = new QuestionSubmittedForReviewEvent("e9", Instant.now(), "q1", "autor1", "¿P?", "C", "T", "S", "BAJO");
        assertTrue(service.handle(other).isEmpty());
        NotificationService full = new NotificationService(List.of(new QuestionSubmittedTemplate()),
                new FakeUserDirectory(), sender, repository);
        assertEquals(1, full.handle(other).size());
    }

    @Test
    void inboxAndMarkReadAreScopedToTheRecipient() {
        service.handle(assigned);
        assertEquals(1, service.inbox("rev1", 1, 10).totalItems());
        assertEquals(1, service.unreadCount("rev1"));
        Long id = service.inbox("rev1", 1, 10).items().get(0).getId();
        assertThrows(IllegalArgumentException.class, () -> service.markRead(id, "rev2"));
        assertTrue(service.markRead(id, "rev1").isRead());
        assertEquals(0, service.unreadCount("rev1"));
    }
}
