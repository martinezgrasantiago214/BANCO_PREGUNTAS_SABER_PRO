package co.unicauca.saberpro.notifications.infrastructure.email;

import co.unicauca.saberpro.notifications.application.port.EmailSenderPort;
import co.unicauca.saberpro.notifications.domain.DeliveryStatus;
import co.unicauca.saberpro.notifications.domain.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Patron Adapter (GoF, estructural): adapta el sistema de archivos a la
 * misma interfaz {@link EmailSenderPort}. Se usa en desarrollo y en la demo
 * para no depender de un servidor SMTP: cada correo queda como .txt en la
 * carpeta "outbox".
 */
public class OutboxFileEmailSenderAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(OutboxFileEmailSenderAdapter.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final Path outbox;
    /** Latencia artificial (ms) para emular un SMTP real en las pruebas de escalabilidad. */
    private final long simulatedLatencyMs;

    public OutboxFileEmailSenderAdapter(Path outbox) {
        this(outbox, 0);
    }

    public OutboxFileEmailSenderAdapter(Path outbox, long simulatedLatencyMs) {
        this.outbox = outbox;
        this.simulatedLatencyMs = Math.max(0, simulatedLatencyMs);
    }

    @Override
    public DeliveryStatus send(EmailMessage message) {
        try {
            if (simulatedLatencyMs > 0) {
                Thread.sleep(simulatedLatencyMs);
            }
            Files.createDirectories(outbox);
            String name = LocalDateTime.now().format(STAMP) + "_" + message.to().replaceAll("[^a-zA-Z0-9._-]", "_")
                    + "_" + UUID.randomUUID().toString().substring(0, 8) + ".txt";
            String content = "Para: " + message.to() + "\nAsunto: " + message.subject() + "\n\n" + message.body();
            Files.writeString(outbox.resolve(name), content, StandardCharsets.UTF_8);
            log.info("Correo simulado para {} -> {}", message.to(), outbox.resolve(name));
            return DeliveryStatus.SIMULADA;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envío interrumpido", e);
        }
    }
}
