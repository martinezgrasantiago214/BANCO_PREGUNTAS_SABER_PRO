package co.unicauca.saberpro.notification.channel;

import co.unicauca.saberpro.notification.domain.DeliveryResult;
import co.unicauca.saberpro.notification.domain.Notification;

/**
 * Interfaz objetivo ("Target") del patron Adapter: la unica forma en que el
 * servicio de notificaciones entrega un mensaje, sin importar si detras hay
 * un servidor SMTP, un archivo o, en el futuro, otro proveedor.
 */
public interface NotificationChannel {

    String name();

    DeliveryResult send(Notification notification);
}
