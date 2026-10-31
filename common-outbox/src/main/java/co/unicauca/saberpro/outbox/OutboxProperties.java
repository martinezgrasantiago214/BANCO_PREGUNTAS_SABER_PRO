package co.unicauca.saberpro.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuracion del relay ({@code saberpro.outbox.*}).
 *
 * @param enabled         si es false no se programa el relay (util en pruebas)
 * @param pollInterval    cada cuanto se buscan mensajes pendientes
 * @param batchSize       maximo de mensajes por ciclo
 * @param confirmTimeout  espera maxima del publisher confirm de RabbitMQ
 * @param retention       cuanto se conservan los mensajes ya publicados (auditoria)
 * @param maxPendingAge   antiguedad a partir de la cual el health pasa a OUT_OF_SERVICE
 * @param maxBackoff      espera maxima entre reintentos cuando el broker no responde
 */
@ConfigurationProperties(prefix = "saberpro.outbox")
public record OutboxProperties(
        Boolean enabled,
        Duration pollInterval,
        Integer batchSize,
        Duration confirmTimeout,
        Duration retention,
        Duration maxPendingAge,
        Duration maxBackoff) {

    public OutboxProperties {
        enabled = enabled == null || enabled;
        pollInterval = pollInterval == null ? Duration.ofMillis(500) : pollInterval;
        batchSize = batchSize == null ? 50 : batchSize;
        confirmTimeout = confirmTimeout == null ? Duration.ofSeconds(5) : confirmTimeout;
        retention = retention == null ? Duration.ofDays(7) : retention;
        maxPendingAge = maxPendingAge == null ? Duration.ofMinutes(2) : maxPendingAge;
        maxBackoff = maxBackoff == null ? Duration.ofSeconds(30) : maxBackoff;
    }
}
