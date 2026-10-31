package co.unicauca.saberpro.notifications.application.template;

import co.unicauca.saberpro.events.ReviewersAssignedEvent;
import co.unicauca.saberpro.notifications.application.port.UserDirectoryPort;
import co.unicauca.saberpro.notifications.domain.Recipient;

import java.util.ArrayList;
import java.util.List;

/** HU04: notifica a cada revisor asignado y le informa al autor que su pregunta esta en revision. */
public class ReviewersAssignedTemplate extends NotificationTemplate<ReviewersAssignedEvent> {

    public ReviewersAssignedTemplate() {
        super(ReviewersAssignedEvent.class);
    }

    @Override
    protected List<Recipient> recipients(ReviewersAssignedEvent e, UserDirectoryPort directory) {
        List<String> logins = new ArrayList<>(e.reviewerLogins());
        logins.add(e.authorLogin());
        return logins(directory, logins);
    }

    @Override
    protected String subject(ReviewersAssignedEvent e, Recipient r) {
        return isAuthor(e, r) ? "Su pregunta está en revisión" : "Se le asignó una pregunta para revisar";
    }

    @Override
    protected String body(ReviewersAssignedEvent e, Recipient r) {
        if (isAuthor(e, r)) {
            return "Su pregunta \"" + shorten(e.directQuestion()) + "\" fue asignada a "
                    + e.reviewerLogins().size() + " revisor(es). Le avisaremos cuando haya observaciones o decisión.";
        }
        return "El administrador " + e.assignedBy() + " lo asignó como revisor de la pregunta:\n\n  \""
                + shorten(e.directQuestion()) + "\"\n\nIngrese a \"Preguntas asignadas\" para evaluarla.";
    }

    private static boolean isAuthor(ReviewersAssignedEvent e, Recipient r) {
        return r.login().equals(e.authorLogin());
    }
}
