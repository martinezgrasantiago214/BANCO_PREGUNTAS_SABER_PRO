package co.unicauca.saberpro.notifications.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void draftIsUnreadAndConvertsToEmail() {
        Notification n = Notification.draft("e1", "ReviewersAssignedEvent", "q1",
                new Recipient("rev1", "Revisor", "rev1@x.co"), "Asunto", "Cuerpo");
        assertFalse(n.isRead());
        assertNull(n.getDeliveryStatus());
        assertEquals(new EmailMessage("rev1@x.co", "Asunto", "Cuerpo"), n.toEmail());
        n.markDelivered(DeliveryStatus.ENVIADA);
        n.markRead();
        assertTrue(n.isRead());
        assertEquals(DeliveryStatus.ENVIADA, n.getDeliveryStatus());
        assertNotNull(n.getCreatedAt());
    }
}
