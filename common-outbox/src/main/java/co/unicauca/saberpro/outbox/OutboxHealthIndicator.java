package co.unicauca.saberpro.outbox;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

import java.time.Clock;
import java.time.Duration;

/**
 * Expone en {@code /actuator/health} cuantos eventos esperan ser publicados y
 * la antiguedad del mas viejo. Si supera el umbral, el servicio se reporta
 * {@code OUT_OF_SERVICE}: el broker lleva demasiado tiempo inaccesible.
 */
public class OutboxHealthIndicator extends AbstractHealthIndicator {

    private final OutboxRepository repository;
    private final Clock clock;
    private final Duration maxPendingAge;

    public OutboxHealthIndicator(OutboxRepository repository, Clock clock, Duration maxPendingAge) {
        this.repository = repository;
        this.clock = clock;
        this.maxPendingAge = maxPendingAge;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        long pending = repository.countByPublishedAtIsNull();
        builder.withDetail("pending", pending);
        repository.findFirstByPublishedAtIsNullOrderByIdAsc().ifPresentOrElse(oldest -> {
            Duration age = Duration.between(oldest.getCreatedAt(), clock.instant());
            builder.withDetail("oldestPendingSeconds", age.toSeconds());
            if (oldest.getLastError() != null) {
                builder.withDetail("lastError", oldest.getLastError());
            }
            if (age.compareTo(maxPendingAge) > 0) {
                builder.outOfService();
            } else {
                builder.up();
            }
        }, builder::up);
    }
}
