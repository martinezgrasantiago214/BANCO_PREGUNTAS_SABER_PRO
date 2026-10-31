package co.unicauca.saberpro.notifications.infrastructure.persistence;

import co.unicauca.saberpro.notifications.domain.DeliveryStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notifications",
       uniqueConstraints = @UniqueConstraint(name = "uk_event_recipient", columnNames = {"eventId", "recipientLogin"}),
       indexes = @Index(name = "idx_notification_recipient", columnList = "recipientLogin"))
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String eventId;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(length = 36)
    private String questionId;

    @Column(nullable = false, length = 60)
    private String recipientLogin;

    @Column(length = 120)
    private String recipientName;

    @Column(nullable = false, length = 120)
    private String recipientEmail;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus deliveryStatus;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    protected NotificationEntity() {
    }

    public NotificationEntity(Long id, String eventId, String eventType, String questionId, String recipientLogin,
                              String recipientName, String recipientEmail, String subject, String body,
                              Instant createdAt, DeliveryStatus deliveryStatus, boolean read) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.questionId = questionId;
        this.recipientLogin = recipientLogin;
        this.recipientName = recipientName;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.body = body;
        this.createdAt = createdAt;
        this.deliveryStatus = deliveryStatus;
        this.read = read;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getQuestionId() { return questionId; }
    public String getRecipientLogin() { return recipientLogin; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public boolean isRead() { return read; }
}
