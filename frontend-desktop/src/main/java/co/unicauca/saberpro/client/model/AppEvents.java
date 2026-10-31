package co.unicauca.saberpro.client.model;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject del patron Observer en el micro-patron MVC: los controladores
 * avisan aqui cuando algo cambio (una pregunta creada, una revision
 * evaluada, una notificacion nueva que llego por eventos) y TODAS las
 * ventanas suscritas se refrescan, sin conocerse entre si.
 */
public class AppEvents {

    private final List<AppObserver> observers = new CopyOnWriteArrayList<>();

    public void subscribe(AppObserver observer) {
        observers.add(observer);
    }

    public void unsubscribe(AppObserver observer) {
        observers.remove(observer);
    }

    public void publish(AppChange change) {
        SwingUtilities.invokeLater(() -> observers.forEach(o -> o.onChange(change)));
    }

    public int observerCount() {
        return observers.size();
    }
}
