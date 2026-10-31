package co.unicauca.saberpro.notifications.testdoubles;

import co.unicauca.saberpro.notifications.application.PageResult;
import co.unicauca.saberpro.notifications.application.port.NotificationRepositoryPort;
import co.unicauca.saberpro.notifications.domain.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryNotificationRepository implements NotificationRepositoryPort {

    public final List<Notification> data = new ArrayList<>();
    private long seq = 1;

    @Override
    public Notification save(Notification n) {
        if (n.getId() == null) {
            n.setId(seq++);
            data.add(n);
        }
        return n;
    }

    @Override
    public boolean exists(String eventId, String recipientLogin) {
        return data.stream().anyMatch(n -> n.getEventId().equals(eventId)
                && n.getRecipient().login().equals(recipientLogin));
    }

    @Override
    public PageResult<Notification> findByRecipient(String login, int page, int size) {
        List<Notification> mine = data.stream().filter(n -> n.getRecipient().login().equals(login)).toList();
        return new PageResult<>(mine, page, size, mine.size());
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return data.stream().filter(n -> n.getId().equals(id)).findFirst();
    }

    @Override
    public long countUnread(String login) {
        return data.stream().filter(n -> n.getRecipient().login().equals(login) && !n.isRead()).count();
    }
}
