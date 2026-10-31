package co.unicauca.saberpro.events;

import java.time.Instant;

/**
 * Contrato comun de los eventos de integracion que viajan por RabbitMQ.
 * Es el "Published Language" entre los contextos delimitados: los
 * microservicios solo comparten estos records, nunca sus entidades.
 */
public interface IntegrationEvent {

    /** Identificador unico del evento (permite a los consumidores ser idempotentes). */
    String eventId();

    Instant occurredAt();

    String questionId();
}
