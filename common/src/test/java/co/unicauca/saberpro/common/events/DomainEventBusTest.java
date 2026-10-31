package co.unicauca.saberpro.common.events;

import co.unicauca.saberpro.common.messaging.RabbitEventForwarder;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Patron Observer: el bus notifica a todos sus observadores, incluido el que reenvia al broker. */
class DomainEventBusTest {

    private final DomainEvent event = new QuestionCreatedEvent("q-1", "autor1", "¿Pregunta?", "Tema", Instant.now());

    @Test
    void notificaATodosLosObservadoresSuscritos() {
        DomainEventBus bus = new DomainEventBus();
        List<DomainEvent> primero = new ArrayList<>();
        List<DomainEvent> segundo = new ArrayList<>();
        bus.subscribe(primero::add);
        bus.subscribe(segundo::add);

        bus.publish(event);

        assertEquals(List.of(event), primero);
        assertEquals(List.of(event), segundo);
        assertEquals(2, bus.listenerCount());
    }

    @Test
    void unObservadorDesuscritoYaNoRecibeEventos() {
        DomainEventBus bus = new DomainEventBus();
        List<DomainEvent> recibidos = new ArrayList<>();
        DomainEventListener listener = recibidos::add;
        bus.subscribe(listener);
        bus.unsubscribe(listener);

        bus.publish(event);

        assertTrue(recibidos.isEmpty());
    }

    @Test
    void siUnObservadorFallaLosDemasIgualRecibenElEvento() {
        DomainEventBus bus = new DomainEventBus();
        List<DomainEvent> recibidos = new ArrayList<>();
        bus.subscribe(e -> {
            throw new IllegalStateException("broker caido");
        });
        bus.subscribe(new EventLogListener());
        bus.subscribe(recibidos::add);

        bus.publish(event);

        assertEquals(1, recibidos.size());
    }

    @Test
    void elReenviadorPublicaEnElExchangeConLaClaveDelEvento() {
        AmqpTemplate template = mock(AmqpTemplate.class);
        new RabbitEventForwarder(template).onEvent(event);

        verify(template).convertAndSend(EventTopics.EXCHANGE, EventTopics.QUESTION_CREATED, event);
    }
}
