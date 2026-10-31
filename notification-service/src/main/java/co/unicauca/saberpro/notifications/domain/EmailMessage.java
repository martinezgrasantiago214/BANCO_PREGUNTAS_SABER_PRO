package co.unicauca.saberpro.notifications.domain;

/** Mensaje de correo listo para enviar. */
public record EmailMessage(String to, String subject, String body) {
}
