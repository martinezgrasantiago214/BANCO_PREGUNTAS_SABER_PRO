package co.unicauca.saberpro.notification.domain;

/** Datos de contacto de un usuario, obtenidos del user-service. */
public record Recipient(String login, String fullName, String email, String role) {
}
