package co.unicauca.saberpro.infra;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del patron Observer (micro patron MVC). */
class SubjectTest {

    @Test
    void notificaATodosLosObserversRegistrados() {
        Subject subject = new Subject();
        AtomicInteger vista1 = new AtomicInteger();
        AtomicInteger vista2 = new AtomicInteger();
        subject.agregarObserver(vista1::incrementAndGet);
        subject.agregarObserver(vista2::incrementAndGet);

        subject.notificarObservers();

        assertEquals(1, vista1.get());
        assertEquals(1, vista2.get());
    }

    @Test
    void unObserverEliminadoYaNoEsNotificado() {
        Subject subject = new Subject();
        AtomicInteger vista = new AtomicInteger();
        Observer observer = vista::incrementAndGet;
        subject.agregarObserver(observer);
        subject.eliminarObserver(observer);

        subject.notificarObservers();

        assertEquals(0, vista.get());
    }
}
