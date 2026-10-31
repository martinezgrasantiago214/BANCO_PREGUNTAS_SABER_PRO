package co.unicauca.saberpro.notifications.application.template;

import co.unicauca.saberpro.events.ReviewCompletedEvent;
import co.unicauca.saberpro.notifications.application.port.UserDirectoryPort;
import co.unicauca.saberpro.notifications.domain.Recipient;

import java.util.ArrayList;
import java.util.List;

/** HU05: decision final para todos los implicados (autor, revisores y administradores). */
public class ReviewCompletedTemplate extends NotificationTemplate<ReviewCompletedEvent> {

    public ReviewCompletedTemplate() {
        super(ReviewCompletedEvent.class);
    }

    @Override
    protected List<Recipient> recipients(ReviewCompletedEvent e, UserDirectoryPort directory) {
        List<String> logins = new ArrayList<>();
        logins.add(e.authorLogin());
        logins.addAll(e.reviewerLogins());
        List<Recipient> result = new ArrayList<>(logins(directory, logins));
        result.addAll(directory.findByRole(ROLE_ADMIN));
        return result;
    }

    @Override
    protected String subject(ReviewCompletedEvent e, Recipient r) {
        return "Pregunta " + ("APROBADA".equals(e.decision()) ? "aprobada" : "rechazada");
    }

    @Override
    protected String body(ReviewCompletedEvent e, Recipient r) {
        String base = "La revisión de la pregunta \"" + shorten(e.directQuestion()) + "\" finalizó con decisión: "
                + e.decision() + ".";
        if (r.login().equals(e.authorLogin()) && "RECHAZADA".equals(e.decision())) {
            return base + "\n\nRevise las observaciones, corrija la pregunta y envíela de nuevo a revisión.";
        }
        return base;
    }
}
