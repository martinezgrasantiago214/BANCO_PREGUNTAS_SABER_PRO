package co.unicauca.saberpro.common.events;

import java.time.Instant;

/**
 * Evento de dominio que un servicio publica en el broker para que otros
 * servicios reaccionen sin acoplarse a el (arquitectura orientada a eventos).
 */
public interface DomainEvent {

    /** Clave de enrutamiento en el exchange de eventos (ver {@link EventTopics}). */
    String routingKey();

    Instant occurredAt();
}
