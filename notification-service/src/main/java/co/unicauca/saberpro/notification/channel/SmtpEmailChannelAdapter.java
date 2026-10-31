package co.unicauca.saberpro.notification.channel;

import co.unicauca.saberpro.notification.domain.DeliveryResult;
import co.unicauca.saberpro.notification.domain.Notification;

/**
 * Patron Adapter (GoF, estructural): adapta {@link SmtpMailClient} (el
 * cliente SMTP del primer corte, cuya interfaz es send(to, subject, body) y
 * devuelve null o un texto de error) a la interfaz {@link NotificationChannel}
 * que usa el servicio. Asi se reutiliza el cliente sin modificarlo y el
 * servicio no conoce detalles de SMTP.
 */
public class SmtpEmailChannelAdapter implements NotificationChannel {

    public static final String NAME = "SMTP";

    private final SmtpMailClient client;

    public SmtpEmailChannelAdapter(SmtpMailClient client) {
        this.client = client;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public DeliveryResult send(Notification notification) {
        String to = notification.recipientEmail();
        if (to == null || to.isBlank()) {
            return DeliveryResult.failed(NAME, "el destinatario no tiene correo registrado");
        }
        if (!client.isConfigured()) {
            return DeliveryResult.failed(NAME, "no hay servidor SMTP configurado (mail.properties)");
        }
        String error = client.send(to, notification.subject(), notification.body());
        return error == null
                ? DeliveryResult.delivered(NAME, "Enviado por correo a " + to)
                : DeliveryResult.failed(NAME, error);
    }
}
