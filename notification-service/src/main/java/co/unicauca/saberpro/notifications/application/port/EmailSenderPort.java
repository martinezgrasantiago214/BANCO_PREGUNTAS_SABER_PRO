package co.unicauca.saberpro.notifications.application.port;

import co.unicauca.saberpro.notifications.domain.DeliveryStatus;
import co.unicauca.saberpro.notifications.domain.EmailMessage;

/**
 * "Target" del patron Adapter: la interfaz que espera la aplicacion. Los
 * adaptadores concretos traducen esta llamada a la API de cada proveedor
 * (JavaMailSender/SMTP, archivo de bandeja de salida, etc.).
 */
public interface EmailSenderPort {

    DeliveryStatus send(EmailMessage message);
}
