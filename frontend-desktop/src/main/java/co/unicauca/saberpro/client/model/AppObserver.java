package co.unicauca.saberpro.client.model;

/** Observer (GoF): las vistas se suscriben para refrescarse cuando el modelo cambia. */
public interface AppObserver {

    void onChange(AppChange change);
}
