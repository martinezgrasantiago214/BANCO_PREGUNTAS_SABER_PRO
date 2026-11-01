package co.unicauca.saberpro.notification.domain;

/** Mensaje que se le envia a un usuario (revisor, autor o administrador). */
public record Notification(String recipientEmail, String recipientName, String subject, String body) {
}
