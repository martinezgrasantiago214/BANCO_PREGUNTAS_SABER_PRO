package co.unicauca.saberpro.notifications.infrastructure.persistence;

import co.unicauca.saberpro.notifications.application.PageResult;
import co.unicauca.saberpro.notifications.application.port.NotificationRepositoryPort;
import co.unicauca.saberpro.notifications.domain.Notification;
import co.unicauca.saberpro.notifications.domain.Recipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository jpa;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Notification save(Notification n) {
        NotificationEntity saved = jpa.save(new NotificationEntity(n.getId(), n.getEventId(), n.getEventType(),
                n.getQuestionId(), n.getRecipient().login(), n.getRecipient().fullName(), n.getRecipient().email(),
                n.getSubject(), n.getBody(), n.getCreatedAt(), n.getDeliveryStatus(), n.isRead()));
        return toDomain(saved);
    }

    @Override
    public boolean exists(String eventId, String recipientLogin) {
        return jpa.existsByEventIdAndRecipientLogin(eventId, recipientLogin);
    }

    @Override
    public PageResult<Notification> findByRecipient(String login, int page, int size) {
        Page<NotificationEntity> p = jpa.findByRecipientLogin(login,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult<>(p.getContent().stream().map(NotificationRepositoryAdapter::toDomain).toList(),
                page, size, p.getTotalElements());
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return jpa.findById(id).map(NotificationRepositoryAdapter::toDomain);
    }

    @Override
    public long countUnread(String login) {
        return jpa.countByRecipientLoginAndReadFalse(login);
    }

    private static Notification toDomain(NotificationEntity e) {
        return new Notification(e.getId(), e.getEventId(), e.getEventType(), e.getQuestionId(),
                new Recipient(e.getRecipientLogin(), e.getRecipientName(), e.getRecipientEmail()),
                e.getSubject(), e.getBody(), e.getCreatedAt(), e.getDeliveryStatus(), e.isRead());
    }
}
