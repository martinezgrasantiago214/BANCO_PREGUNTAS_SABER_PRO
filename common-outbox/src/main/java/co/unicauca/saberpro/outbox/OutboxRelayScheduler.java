package co.unicauca.saberpro.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Ejecuta el relay en un hilo propio mientras el contexto esta activo. Se usa
 * un executor dedicado (y no {@code @EnableScheduling}) para no alterar la
 * configuracion de tareas programadas del servicio que incluye este modulo.
 */
public class OutboxRelayScheduler implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);
    private static final Duration PURGE_EVERY = Duration.ofHours(1);

    private final OutboxRelay relay;
    private final OutboxProperties properties;
    private ScheduledExecutorService executor;

    public OutboxRelayScheduler(OutboxRelay relay, OutboxProperties properties) {
        this.relay = relay;
        this.properties = properties;
    }

    @Override
    public synchronized void start() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "outbox-relay");
            t.setDaemon(true);
            return t;
        });
        long poll = properties.pollInterval().toMillis();
        executor.scheduleWithFixedDelay(() -> safely(relay::relayPending), poll, poll, TimeUnit.MILLISECONDS);
        executor.scheduleWithFixedDelay(() -> safely(() -> relay.purgePublishedOlderThan(properties.retention())),
                PURGE_EVERY.toMillis(), PURGE_EVERY.toMillis(), TimeUnit.MILLISECONDS);
        log.info("Outbox relay iniciado (cada {} ms, lotes de {})", poll, properties.batchSize());
    }

    @Override
    public synchronized void stop() {
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return executor != null;
    }

    private static void safely(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            // Un error de base de datos no debe matar el hilo programado.
            log.warn("Fallo un ciclo del outbox relay: {}", e.getMessage());
        }
    }
}
