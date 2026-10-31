package co.unicauca.saberpro.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Observador que deja constancia en el log de cada evento publicado. */
public class EventLogListener implements DomainEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogListener.class);

    @Override
    public void onEvent(DomainEvent event) {
        LOGGER.info("Evento de dominio {}: {}", event.routingKey(), event);
    }
}
