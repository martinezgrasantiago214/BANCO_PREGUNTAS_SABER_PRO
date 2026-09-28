package co.unicauca.saberpro.infra;

import java.util.ArrayList;
import java.util.List;

/** Patron Observer: sujeto observable que notifica a sus observadores. */
public class Subject {

    private final List<Observer> observers = new ArrayList<>();

    public void agregarObserver(Observer observer) {
        observers.add(observer);
    }

    public void eliminarObserver(Observer observer) {
        observers.remove(observer);
    }

    public void notificarObservers() {
        for (Observer observer : new ArrayList<>(observers)) {
            observer.actualizar();
        }
    }
}
