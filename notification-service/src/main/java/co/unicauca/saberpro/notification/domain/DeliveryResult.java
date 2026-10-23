package co.unicauca.saberpro.notification.domain;

/** Resultado de intentar entregar una notificacion por un canal. */
public record DeliveryResult(boolean delivered, String channel, String detail) {

    public static DeliveryResult delivered(String channel, String detail) {
        return new DeliveryResult(true, channel, detail);
    }

    public static DeliveryResult failed(String channel, String reason) {
        return new DeliveryResult(false, channel, reason);
    }
}
