package co.unicauca.saberpro.notification.application;

import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.events.ReviewersAssignedEvent;
import co.unicauca.saberpro.common.web.PendingIssueException;
import co.unicauca.saberpro.notification.channel.NotificationChannel;
import co.unicauca.saberpro.notification.directory.UserDirectory;
import co.unicauca.saberpro.notification.domain.DeliveryResult;
import co.unicauca.saberpro.notification.domain.Notification;

/**
 * Envia las notificaciones que disparan los eventos. Intenta primero el canal
 * principal (SMTP) y, si falla, deja el mensaje en el canal de respaldo
 * (bandeja de salida en archivo), como hacia el primer corte.
 */
public class NotificationService {

    private final NotificationChannel primary;
    private final NotificationChannel fallback;
    private final UserDirectory users;

    public NotificationService(NotificationChannel primary, NotificationChannel fallback, UserDirectory users) {
        this.primary = primary;
        this.fallback = fallback;
        this.users = users;
    }

    /** Entrega una notificacion por el canal principal o, si falla, por el de respaldo. */
    public DeliveryResult deliver(Notification notification) {
        DeliveryResult result = primary.send(notification);
        if (result.delivered() || fallback == null) {
            return result;
        }
        DeliveryResult fallbackResult = fallback.send(notification);
        return new DeliveryResult(fallbackResult.delivered(), fallbackResult.channel(),
                fallbackResult.detail() + " (" + primary.name() + ": " + result.detail() + ")");
    }

    /**
     * HU04: avisar por correo a cada revisor asignado.
     *
     * TODO(PSP-34): por cada login de event.reviewerLogins(), buscar su correo con users.findByLogin(...),
     * armar la Notification (pregunta event.directQuestion(), id event.questionId()) y llamar deliver(...).
     */
    public void onReviewersAssigned(ReviewersAssignedEvent event) {
        throw new PendingIssueException("PSP-34", "enviar el correo a los revisores asignados (HU04)");
    }

    /**
     * HU05: avisar al autor y a los administradores del resultado de la evaluacion.
     *
     * TODO(PSP-49): buscar al autor (users.findByLogin(event.authorLogin())) y a los administradores
     * (users.findAdministrators()), y enviarles el veredicto y las observaciones con deliver(...).
     */
    public void onQuestionEvaluated(QuestionEvaluatedEvent event) {
        throw new PendingIssueException("PSP-49", "notificar al autor y al administrador la evaluacion (HU05)");
    }
}
