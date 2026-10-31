package co.unicauca.saberpro.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    /**
     * Toma un lote de mensajes pendientes en orden de escritura y los bloquea.
     * El hint {@code lock.timeout = -2} se traduce a {@code FOR UPDATE SKIP LOCKED}
     * en PostgreSQL: si hay varias replicas del servicio, cada una toma filas
     * distintas en lugar de esperar o duplicar el envio.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select m from OutboxMessage m where m.publishedAt is null order by m.id asc")
    List<OutboxMessage> lockNextPending(Pageable page);

    long countByPublishedAtIsNull();

    Optional<OutboxMessage> findFirstByPublishedAtIsNullOrderByIdAsc();

    Optional<OutboxMessage> findByEventId(String eventId);

    @Modifying
    @Query("delete from OutboxMessage m where m.publishedAt is not null and m.publishedAt < :threshold")
    int deletePublishedBefore(Instant threshold);
}
