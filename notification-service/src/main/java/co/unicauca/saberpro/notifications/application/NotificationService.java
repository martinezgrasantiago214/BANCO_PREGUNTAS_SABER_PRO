package co.unicauca.saberpro.notifications.application;

import co.unicauca.saberpro.events.IntegrationEvent;
import co.unicauca.saberpro.notifications.application.port.EmailSenderPort;
import co.unicauca.saberpro.notifications.application.port.NotificationRepositoryPort;
import co.unicauca.saberpro.notifications.application.port.UserDirectoryPort;
import co.unicauca.saberpro.notifications.application.template.NotificationTemplate;
import co.unicauca.saberpro.notifications.domain.DeliveryStatus;
import co.unicauca.saberpro.notifications.domain.Notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Casos de uso: notificar a los implicados ante cada evento y consultar la bandeja. */
public class NotificationService {

    private final Map<Class<?>, NotificationTemplate<?>> templates = new HashMap<>();
    private final UserDirectoryPort directory;
    private final EmailSenderPort emailSender;
    private final NotificationRepositoryPort repository;

    public NotificationService(List<NotificationTemplate<?>> templates, UserDirectoryPort directory,
                               EmailSenderPort emailSender, NotificationRepositoryPort repository) {
        templates.forEach(t -> this.templates.put(t.eventType(), t));
        this.directory = directory;
        this.emailSender = emailSender;
        this.repository = repository;
    }

    /** @return notificaciones generadas (vacia si el evento no interesa o ya se proceso). */
    public List<Notification> handle(IntegrationEvent event) {
        NotificationTemplate<?> template = templates.get(event.getClass());
        if (template == null) {
            return List.of();
        }
        List<Notification> sent = new ArrayList<>();
        for (Notification n : template.compose(event, directory)) {
            if (repository.exists(n.getEventId(), n.getRecipient().login())) {
                continue; // evento re-entregado: no se notifica dos veces
            }
            DeliveryStatus status;
            try {
                status = emailSender.send(n.toEmail());
            } catch (RuntimeException ex) {
                status = DeliveryStatus.FALLIDA;
            }
            n.markDelivered(status);
            sent.add(repository.save(n));
        }
        return sent;
    }

    public PageResult<Notification> inbox(String login, int page, int size) {
        return repository.findByRecipient(login, Math.max(1, page), Math.min(50, Math.max(1, size)));
    }

    public long unreadCount(String login) {
        return repository.countUnread(login);
    }

    public Notification markRead(Long id, String login) {
        Notification n = repository.findById(id)
                .filter(x -> x.getRecipient().login().equals(login))
                .orElseThrow(() -> new IllegalArgumentException("No existe la notificación " + id + " para " + login));
        n.markRead();
        return repository.save(n);
    }
}
