package co.unicauca.saberpro.notifications.application.template;

import co.unicauca.saberpro.events.ObservationAddedEvent;
import co.unicauca.saberpro.notifications.application.port.UserDirectoryPort;
import co.unicauca.saberpro.notifications.domain.Recipient;

import java.util.List;

/** HU05: el autor recibe las observaciones que deja el revisor. */
public class ObservationAddedTemplate extends NotificationTemplate<ObservationAddedEvent> {

    public ObservationAddedTemplate() {
        super(ObservationAddedEvent.class);
    }

    @Override
    protected List<Recipient> recipients(ObservationAddedEvent e, UserDirectoryPort directory) {
        return logins(directory, List.of(e.authorLogin()));
    }

    @Override
    protected String subject(ObservationAddedEvent e, Recipient r) {
        return "Nueva observación sobre su pregunta";
    }

    @Override
    protected String body(ObservationAddedEvent e, Recipient r) {
        return "El revisor " + e.reviewerLogin() + " dejó una observación sobre \"" + shorten(e.directQuestion())
                + "\":\n\n  " + e.text();
    }
}
