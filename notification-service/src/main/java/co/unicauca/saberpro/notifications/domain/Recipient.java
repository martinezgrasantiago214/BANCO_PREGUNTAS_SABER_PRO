package co.unicauca.saberpro.notifications.domain;

/** Destinatario resuelto a partir del contexto de usuarios. */
public record Recipient(String login, String fullName, String email) {
}
