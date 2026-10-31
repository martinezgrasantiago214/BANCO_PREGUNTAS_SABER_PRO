package co.unicauca.saberpro.notifications.application.template;

import co.unicauca.saberpro.events.IntegrationEvent;
import co.unicauca.saberpro.notifications.application.port.UserDirectoryPort;
import co.unicauca.saberpro.notifications.domain.Notification;
import co.unicauca.saberpro.notifications.domain.Recipient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Patron Template Method (GoF, comportamiento). {@link #compose} fija el
 * algoritmo para construir las notificaciones de un evento: resolver
 * destinatarios, quitar duplicados, redactar asunto y cuerpo, y firmar el
 * mensaje. Cada subclase solo implementa los pasos variables
 * ({@link #recipients}, {@link #subject}, {@link #body}).
 *
 * Agregar una notificacion nueva = agregar una subclase (OCP).
 */
public abstract class NotificationTemplate<E extends IntegrationEvent> {

    protected static final String ROLE_ADMIN = "ADMINISTRADOR";
    private static final String SIGNATURE = "\n\n--\nBanco de Preguntas Saber PRO - Universidad del Cauca\n"
            + "Este es un mensaje automático, por favor no responda.";

    private final Class<E> eventType;

    protected NotificationTemplate(Class<E> eventType) {
        this.eventType = eventType;
    }

    public Class<E> eventType() {
        return eventType;
    }

    /** Metodo plantilla: el esqueleto no cambia entre tipos de evento. */
    public final List<Notification> compose(IntegrationEvent rawEvent, UserDirectoryPort directory) {
        E event = eventType.cast(rawEvent);
        Map<String, Recipient> unique = new LinkedHashMap<>();
        for (Recipient r : recipients(event, directory)) {
            if (r != null && r.email() != null && !r.email().isBlank()) {
                unique.putIfAbsent(r.login(), r);
            }
        }
        List<Notification> result = new ArrayList<>();
        for (Recipient recipient : unique.values()) {
            String body = "Hola " + recipient.fullName() + ",\n\n" + body(event, recipient) + SIGNATURE;
            result.add(Notification.draft(event.eventId(), eventType.getSimpleName(), event.questionId(),
                    recipient, "[Saber PRO] " + subject(event, recipient), body));
        }
        return result;
    }

    protected abstract List<Recipient> recipients(E event, UserDirectoryPort directory);

    protected abstract String subject(E event, Recipient recipient);

    protected abstract String body(E event, Recipient recipient);

    // ---------------------------------------------------------------- utilidades para las subclases

    protected static List<Recipient> logins(UserDirectoryPort directory, List<String> logins) {
        List<Recipient> result = new ArrayList<>();
        for (String login : logins) {
            directory.findByLogin(login).ifPresent(result::add);
        }
        return result;
    }

    protected static String shorten(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 120 ? text : text.substring(0, 117) + "...";
    }
}
