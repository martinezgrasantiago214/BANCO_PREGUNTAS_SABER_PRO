package co.unicauca.saberpro.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Patron Observer (GoF, comportamiento): sujeto al que se suscriben los
 * observadores de eventos de dominio. El servicio de aplicacion solo publica
 * aqui ("paso X") sin saber quien escucha; uno de los observadores reenvia el
 * evento al broker RabbitMQ para los demas microservicios, otro lo registra
 * en el log, y se pueden agregar mas sin tocar el servicio.
 *
 * Si un observador falla, los demas igual reciben el evento.
 */
public class DomainEventBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainEventBus.class);

    private final List<DomainEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(DomainEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(DomainEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(DomainEvent event) {
        for (DomainEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException ex) {
                LOGGER.error("El observador {} fallo al procesar {}",
                        listener.getClass().getSimpleName(), event.routingKey(), ex);
            }
        }
    }

    public int listenerCount() {
        return listeners.size();
    }
}
