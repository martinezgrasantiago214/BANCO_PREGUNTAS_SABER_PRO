package co.unicauca.saberpro.review.testdoubles;

import co.unicauca.saberpro.review.application.port.out.ReviewEventPublisher;
import co.unicauca.saberpro.review.domain.event.ReviewDomainEvent;

import java.util.ArrayList;
import java.util.List;

/** Doble del puerto de eventos: recuerda lo que se publico. */
public class RecordingEventPublisher implements ReviewEventPublisher {

    public final List<ReviewDomainEvent> published = new ArrayList<>();

    @Override
    public void publish(List<ReviewDomainEvent> events) {
        published.addAll(events);
    }

    public <T extends ReviewDomainEvent> List<T> ofType(Class<T> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
