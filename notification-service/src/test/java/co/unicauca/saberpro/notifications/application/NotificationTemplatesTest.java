package co.unicauca.saberpro.notifications.application;

import co.unicauca.saberpro.events.*;
import co.unicauca.saberpro.notifications.application.template.*;
import co.unicauca.saberpro.notifications.domain.Notification;
import co.unicauca.saberpro.notifications.testdoubles.FakeUserDirectory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Template Method: cada plantilla solo define destinatarios, asunto y cuerpo. */
class NotificationTemplatesTest {

    private final FakeUserDirectory directory = new FakeUserDirectory();

    private static List<String> logins(List<Notification> list) {
        return list.stream().map(n -> n.getRecipient().login()).toList();
    }

    @Test
    void questionSubmittedNotifiesAdmins() {
        var e = new QuestionSubmittedForReviewEvent("e1", Instant.now(), "q1", "autor1", "¿Pregunta?", "C", "T", "S", "MEDIO");
        List<Notification> list = new QuestionSubmittedTemplate().compose(e, directory);
        assertEquals(List.of("admin"), logins(list));
        assertTrue(list.get(0).getSubject().startsWith("[Saber PRO]"));
        assertTrue(list.get(0).getBody().startsWith("Hola Admin,"));
    }

    @Test
    void reviewersAssignedNotifiesReviewersAndAuthorWithDifferentMessages() {
        var e = new ReviewersAssignedEvent("e2", Instant.now(), "q1", "autor1", "¿Pregunta?", List.of("rev1", "rev2"), "admin");
        List<Notification> list = new ReviewersAssignedTemplate().compose(e, directory);
        assertEquals(List.of("rev1", "rev2", "autor1"), logins(list));
        assertTrue(list.get(0).getSubject().contains("asignó"));
        assertTrue(list.get(2).getSubject().contains("en revisión"));
    }

    @Test
    void evaluationAndObservationNotifyTheAuthor() {
        var ev = new ReviewEvaluationSubmittedEvent("e3", Instant.now(), "q1", "autor1", "¿P?", "rev1", "APROBADA", 4.5);
        assertEquals(List.of("autor1"), logins(new EvaluationSubmittedTemplate().compose(ev, directory)));
        assertTrue(new EvaluationSubmittedTemplate().compose(ev, directory).get(0).getBody().contains("4.5"));
        var ob = new ObservationAddedEvent("e4", Instant.now(), "q1", "autor1", "¿P?", "rev1", "Mejorar el distractor C");
        assertTrue(new ObservationAddedTemplate().compose(ob, directory).get(0).getBody().contains("Mejorar el distractor C"));
    }

    @Test
    void reviewCompletedNotifiesEveryoneInvolvedOnce() {
        var e = new ReviewCompletedEvent("e5", Instant.now(), "q1", "autor1", "¿P?", "RECHAZADA", List.of("rev1", "admin"));
        List<Notification> list = new ReviewCompletedTemplate().compose(e, directory);
        assertEquals(List.of("autor1", "rev1", "admin"), logins(list), "admin no se duplica");
        assertTrue(list.get(0).getBody().contains("corrija"));
        assertEquals("[Saber PRO] Pregunta rechazada", list.get(0).getSubject());
    }

    @Test
    void recipientsWithoutEmailOrUnknownAreSkipped() {
        var e = new ObservationAddedEvent("e6", Instant.now(), "q1", "sincorreo", "¿P?", "rev1", "x");
        assertTrue(new ObservationAddedTemplate().compose(e, directory).isEmpty());
        var e2 = new ObservationAddedEvent("e7", Instant.now(), "q1", "desconocido", "¿P?", "rev1", "x");
        assertTrue(new ObservationAddedTemplate().compose(e2, directory).isEmpty());
    }
}
