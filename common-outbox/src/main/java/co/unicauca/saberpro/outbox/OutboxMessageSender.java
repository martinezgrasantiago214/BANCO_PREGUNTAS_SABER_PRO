package co.unicauca.saberpro.outbox;

/**
 * Puerto hacia el broker. Debe lanzar una excepcion si el broker no confirma
 * la recepcion, para que el relay deje el mensaje pendiente y lo reintente.
 */
@FunctionalInterface
public interface OutboxMessageSender {

    void send(String routingKey, Object event) throws Exception;
}
