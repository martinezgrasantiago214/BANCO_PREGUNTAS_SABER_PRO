package co.unicauca.saberpro.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Fila de la tabla outbox: un evento de integracion que ya ocurrio en el
 * dominio (porque se guardo en la MISMA transaccion que el agregado) y que
 * aun debe llegar al broker.
 *
 * <p>La clave es una secuencia: define el orden de entrega, incluso cuando
 * un caso de uso emite varios eventos en el mismo instante. El
 * {@code eventId} se conserva, de modo que un reenvio produce exactamente el
 * mismo mensaje y los consumidores lo deduplican.</p>
 */
@Entity
@Table(name = "outbox_message",
       indexes = @Index(name = "ix_outbox_pending", columnList = "publishedAt, id"))
public class OutboxMessage {

    static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(nullable = false, length = 80)
    private String routingKey;

    /** Nombre completo de la clase del evento, para reconstruirlo al reenviar. */
    @Column(nullable = false, length = 200)
    private String eventType;

    @Column(length = 64)
    private String aggregateId;

    /** JSON del evento. Se usa varchar (no @Lob) para que PostgreSQL no lo guarde como large object. */
    @Column(nullable = false, length = 16000)
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    private int attempts;

    @Column(length = MAX_ERROR_LENGTH)
    private String lastError;

    protected OutboxMessage() {
    }

    public OutboxMessage(String eventId, String routingKey, String eventType, String aggregateId,
                         String payload, Instant createdAt) {
        this.eventId = eventId;
        this.routingKey = routingKey;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void markPublished(Instant when) {
        this.publishedAt = when;
        this.attempts++;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error == null ? null
                : error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
    }

    public boolean isPublished() {
        return publishedAt != null;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getRoutingKey() { return routingKey; }
    public String getEventType() { return eventType; }
    public String getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
}
