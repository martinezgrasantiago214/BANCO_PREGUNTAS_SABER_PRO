package co.unicauca.saberpro.notification.channel;

import co.unicauca.saberpro.notification.domain.DeliveryResult;
import co.unicauca.saberpro.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Canal de respaldo: guarda la notificacion como .txt en una bandeja de
 * salida local. Permite verificar las notificaciones sin un servidor SMTP.
 */
public class OutboxFileChannel implements NotificationChannel {

    public static final String NAME = "OUTBOX";
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxFileChannel.class);

    private final File outboxDir;

    public OutboxFileChannel(File outboxDir) {
        this.outboxDir = outboxDir;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public DeliveryResult send(Notification notification) {
        String to = notification.recipientEmail();
        outboxDir.mkdirs();
        File file = new File(outboxDir, "email_" + System.currentTimeMillis() + "_"
                + (to == null ? "sin-correo" : to.replaceAll("[^A-Za-z0-9@._-]", "_")) + ".txt");
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.println("Fecha: " + LocalDateTime.now());
            writer.println("Para: " + notification.recipientName() + " <" + to + ">");
            writer.println("Asunto: " + notification.subject());
            writer.println();
            writer.println(notification.body());
        } catch (IOException ex) {
            LOGGER.warn("No fue posible escribir en la bandeja de salida {}", outboxDir, ex);
            return DeliveryResult.failed(NAME, ex.getMessage());
        }
        LOGGER.info("[EMAIL SIMULADO] Notificacion registrada en {}", file.getAbsolutePath());
        return DeliveryResult.delivered(NAME, "Simulado para " + to + " -> " + file.getAbsolutePath());
    }
}
