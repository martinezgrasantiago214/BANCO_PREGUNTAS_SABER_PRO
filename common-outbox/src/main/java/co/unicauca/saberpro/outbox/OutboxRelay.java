package co.unicauca.saberpro.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Lado de lectura del patron Transactional Outbox (<i>message relay</i> por
 * sondeo). Cada ciclo bloquea un lote de mensajes pendientes, los envia al
 * broker en orden y los marca como publicados en la misma transaccion.
 *
 * <p>Si un envio falla, el ciclo se detiene para no alterar el orden de los
 * eventos; el mensaje queda pendiente con su error y el relay espera cada vez
 * mas (backoff exponencial, hasta {@code maxBackoff}) antes de reintentar,
 * para no saturar el log ni el broker mientras se recupera. Si el broker
 * confirma pero la base falla al marcar, el evento se reenviara: por eso los
 * consumidores deduplican por {@code eventId}.</p>
 */
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final OutboxMessageSender sender;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final int batchSize;
    private final Duration baseBackoff;
    private final Duration maxBackoff;

    private int consecutiveFailures;
    private Instant nextAttemptAt = Instant.MIN;

    public OutboxRelay(OutboxRepository repository, OutboxMessageSender sender, ObjectMapper objectMapper,
                       TransactionTemplate tx, Clock clock, int batchSize, Duration baseBackoff,
                       Duration maxBackoff) {
        this.repository = repository;
        this.sender = sender;
        this.objectMapper = objectMapper;
        this.tx = tx;
        this.clock = clock;
        this.batchSize = batchSize;
        this.baseBackoff = baseBackoff;
        this.maxBackoff = maxBackoff;
    }

    /** Ejecuta un ciclo y devuelve cuantos mensajes se publicaron. */
    public synchronized int relayPending() {
        if (clock.instant().isBefore(nextAttemptAt)) {
            return 0;
        }
        Cycle cycle = tx.execute(status -> {
            List<OutboxMessage> batch = repository.lockNextPending(PageRequest.of(0, batchSize));
            int sent = 0;
            for (OutboxMessage message : batch) {
                try {
                    Object event = objectMapper.readValue(message.getPayload(), Class.forName(message.getEventType()));
                    sender.send(message.getRoutingKey(), event);
                    message.markPublished(clock.instant());
                    sent++;
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    message.markFailed(e.getClass().getSimpleName() + ": " + e.getMessage());
                    return new Cycle(sent, message, e);
                }
            }
            return new Cycle(sent, null, null);
        });
        if (cycle == null) {
            return 0;
        }
        if (cycle.failed != null) {
            onFailure(cycle.failed, cycle.error);
        } else if (consecutiveFailures > 0) {
            log.info("Outbox: conexión con el broker recuperada tras {} intento(s) fallidos", consecutiveFailures);
            consecutiveFailures = 0;
            nextAttemptAt = Instant.MIN;
        }
        if (cycle.sent > 0) {
            log.info("Outbox: {} evento(s) publicados en RabbitMQ", cycle.sent);
        }
        return cycle.sent;
    }

    /** Borra los mensajes ya publicados hace mas de {@code retention}. */
    public int purgePublishedOlderThan(Duration retention) {
        Integer deleted = tx.execute(status -> repository.deletePublishedBefore(clock.instant().minus(retention)));
        return deleted == null ? 0 : deleted;
    }

    private void onFailure(OutboxMessage message, Exception error) {
        consecutiveFailures++;
        Duration wait = backoffFor(consecutiveFailures);
        nextAttemptAt = clock.instant().plus(wait);
        log.warn("No se pudo publicar {} (evento {}), intento {}. Se reintenta en {} ms: {}",
                message.getRoutingKey(), message.getEventId(), message.getAttempts(), wait.toMillis(),
                error.getMessage());
    }

    /** base * 2^(n-1), con tope en maxBackoff. */
    Duration backoffFor(int failures) {
        int exponent = Math.min(failures - 1, 20);
        Duration wait = baseBackoff.multipliedBy(1L << exponent);
        return wait.compareTo(maxBackoff) > 0 ? maxBackoff : wait;
    }

    private record Cycle(int sent, OutboxMessage failed, Exception error) {
    }
}
