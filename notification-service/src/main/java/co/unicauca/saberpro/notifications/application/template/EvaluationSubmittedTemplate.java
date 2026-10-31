package co.unicauca.saberpro.notifications.application.template;

import co.unicauca.saberpro.events.ReviewEvaluationSubmittedEvent;
import co.unicauca.saberpro.notifications.application.port.UserDirectoryPort;
import co.unicauca.saberpro.notifications.domain.Recipient;

import java.util.List;
import java.util.Locale;

/** HU05: informa al autor que un revisor registro su concepto. */
public class EvaluationSubmittedTemplate extends NotificationTemplate<ReviewEvaluationSubmittedEvent> {

    public EvaluationSubmittedTemplate() {
        super(ReviewEvaluationSubmittedEvent.class);
    }

    @Override
    protected List<Recipient> recipients(ReviewEvaluationSubmittedEvent e, UserDirectoryPort directory) {
        return logins(directory, List.of(e.authorLogin()));
    }

    @Override
    protected String subject(ReviewEvaluationSubmittedEvent e, Recipient r) {
        return "Un revisor evaluó su pregunta";
    }

    @Override
    protected String body(ReviewEvaluationSubmittedEvent e, Recipient r) {
        return "El revisor " + e.reviewerLogin() + " registró su evaluación de la pregunta \""
                + shorten(e.directQuestion()) + "\".\n\nConcepto: " + e.verdict()
                + "\nPromedio de la rúbrica: " + String.format(Locale.ROOT, "%.1f", e.averageScore()) + " / 5";
    }
}
