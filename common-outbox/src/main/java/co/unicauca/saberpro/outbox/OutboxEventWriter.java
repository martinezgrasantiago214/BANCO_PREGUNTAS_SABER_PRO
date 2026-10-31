package co.unicauca.saberpro.outbox;

import co.unicauca.saberpro.events.IntegrationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Lado de escritura del patron Transactional Outbox. Los adaptadores de
 * mensajeria de cada servicio lo usan en lugar de hablar con RabbitMQ.
 *
 * <p>La propagacion {@code MANDATORY} es deliberada: si alguien intenta
 * encolar un evento fuera de la transaccion del caso de uso, falla de
 * inmediato en lugar de perder la atomicidad en silencio.</p>
 */
public class OutboxEventWriter {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxEventWriter(OutboxRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String routingKey, IntegrationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            repository.save(new OutboxMessage(event.eventId(), routingKey, event.getClass().getName(),
                    event.questionId(), payload, clock.instant()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + event.getClass().getSimpleName(), e);
        }
    }
}
