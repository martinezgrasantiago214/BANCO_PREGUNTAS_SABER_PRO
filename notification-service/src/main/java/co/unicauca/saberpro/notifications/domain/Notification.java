package co.unicauca.saberpro.notifications.domain;

import java.time.Instant;

/**
 * Notificacion dirigida a un usuario. Ademas del correo, queda registrada
 * para que el usuario la vea en su bandeja dentro de la aplicacion.
 */
public class Notification {

    private Long id;
    private final String eventId;
    private final String eventType;
    private final String questionId;
    private final Recipient recipient;
    private final String subject;
    private final String body;
    private final Instant createdAt;
    private DeliveryStatus deliveryStatus;
    private boolean read;

    public Notification(Long id, String eventId, String eventType, String questionId, Recipient recipient,
                        String subject, String body, Instant createdAt, DeliveryStatus deliveryStatus, boolean read) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.questionId = questionId;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.deliveryStatus = deliveryStatus;
        this.read = read;
    }

    public static Notification draft(String eventId, String eventType, String questionId, Recipient recipient,
                                     String subject, String body) {
        return new Notification(null, eventId, eventType, questionId, recipient, subject, body, null, null, false);
    }

    public EmailMessage toEmail() {
        return new EmailMessage(recipient.email(), subject, body);
    }

    public void markDelivered(DeliveryStatus status) {
        this.deliveryStatus = status;
    }

    public void markRead() {
        this.read = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getQuestionId() { return questionId; }
    public Recipient getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public boolean isRead() { return read; }
}
