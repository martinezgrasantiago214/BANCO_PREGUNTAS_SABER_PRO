package co.unicauca.saberpro.client;

import co.unicauca.saberpro.client.api.SaberProApiFacade;
import co.unicauca.saberpro.client.api.http.ApiException;
import co.unicauca.saberpro.client.controller.ReviewController;
import co.unicauca.saberpro.client.model.AppEvents;
import co.unicauca.saberpro.client.model.AppObserver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientLogicTest {

    @Test
    void reviewerSelectionMustBeBetweenOneAndThree() {
        ReviewController controller = new ReviewController(new SaberProApiFacade("http://localhost:1"), new AppEvents());
        assertNotNull(controller.validateSelection(List.of()));
        assertNull(controller.validateSelection(List.of("rev1")));
        assertNull(controller.validateSelection(List.of("rev1", "rev2", "rev3")));
        assertNotNull(controller.validateSelection(List.of("a", "b", "c", "d")));
    }

    @Test
    void apiExceptionBuildsReadableMessage() {
        ApiException ex = new ApiException(400, "La pregunta no cumple la validación estructural.",
                List.of("El contexto es obligatorio.", "Debe indicar la respuesta correcta."));
        assertEquals("La pregunta no cumple la validación estructural.\n • El contexto es obligatorio."
                + "\n • Debe indicar la respuesta correcta.", ex.toUserMessage());
        assertEquals(400, ex.getStatus());
    }

    @Test
    void observersCanSubscribeAndUnsubscribe() {
        AppEvents events = new AppEvents();
        AppObserver observer = change -> { };
        events.subscribe(observer);
        assertEquals(1, events.observerCount());
        events.unsubscribe(observer);
        assertEquals(0, events.observerCount());
    }

    @Test
    void facadeStartsWithoutSession() {
        assertTrue(new SaberProApiFacade("http://localhost:1").currentUser().isEmpty());
    }
}
