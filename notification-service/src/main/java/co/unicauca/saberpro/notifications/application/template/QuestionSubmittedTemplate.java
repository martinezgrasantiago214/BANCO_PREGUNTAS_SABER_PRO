package co.unicauca.saberpro.notifications.application.template;

import co.unicauca.saberpro.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.notifications.application.port.UserDirectoryPort;
import co.unicauca.saberpro.notifications.domain.Recipient;

import java.util.List;

/** HU02: avisa a los administradores que hay una pregunta esperando revisores. */
public class QuestionSubmittedTemplate extends NotificationTemplate<QuestionSubmittedForReviewEvent> {

    public QuestionSubmittedTemplate() {
        super(QuestionSubmittedForReviewEvent.class);
    }

    @Override
    protected List<Recipient> recipients(QuestionSubmittedForReviewEvent e, UserDirectoryPort directory) {
        return directory.findByRole(ROLE_ADMIN);
    }

    @Override
    protected String subject(QuestionSubmittedForReviewEvent e, Recipient r) {
        return "Nueva pregunta pendiente de asignar revisores";
    }

    @Override
    protected String body(QuestionSubmittedForReviewEvent e, Recipient r) {
        return "El autor " + e.authorLogin() + " envió a revisión la pregunta:\n\n  \"" + shorten(e.directQuestion())
                + "\"\n\nCompetencia: " + e.competence() + "\nTema: " + e.topic()
                + "\n\nIngrese a la aplicación para asignarle entre 1 y 3 revisores.";
    }
}
