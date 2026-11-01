package co.unicauca.saberpro.common.events;


/** Patron Observer: observador que reacciona a los eventos de dominio del question-service. */
public interface DomainEventListener {

    void onEvent(DomainEvent event);
}
