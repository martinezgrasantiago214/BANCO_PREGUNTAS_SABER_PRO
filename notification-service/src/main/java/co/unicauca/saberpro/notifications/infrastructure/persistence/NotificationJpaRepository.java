package co.unicauca.saberpro.notifications.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    boolean existsByEventIdAndRecipientLogin(String eventId, String recipientLogin);

    Page<NotificationEntity> findByRecipientLogin(String recipientLogin, Pageable pageable);

    long countByRecipientLoginAndReadFalse(String recipientLogin);
}
