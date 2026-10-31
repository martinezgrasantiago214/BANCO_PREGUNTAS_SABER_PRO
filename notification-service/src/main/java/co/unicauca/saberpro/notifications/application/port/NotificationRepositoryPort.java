package co.unicauca.saberpro.notifications.application.port;

import co.unicauca.saberpro.notifications.application.PageResult;
import co.unicauca.saberpro.notifications.domain.Notification;

import java.util.Optional;

public interface NotificationRepositoryPort {

    Notification save(Notification notification);

    /** Idempotencia: el mismo evento no notifica dos veces al mismo usuario. */
    boolean exists(String eventId, String recipientLogin);

    PageResult<Notification> findByRecipient(String login, int page, int size);

    Optional<Notification> findById(Long id);

    long countUnread(String login);
}
