package co.unicauca.saberpro.users.domain;

import co.unicauca.saberpro.users.domain.model.Role;
import co.unicauca.saberpro.users.domain.model.User;
import co.unicauca.saberpro.users.domain.model.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Un usuario nuevo queda activo por defecto y conserva su rol")
    void defaultsToActive() {
        User user = new User(null, " rev1 ", "Revisor", "rev1@x.co", Role.REVISOR, null, "h");
        assertTrue(user.isActive());
        assertEquals("rev1", user.getLogin());
        assertTrue(user.hasRole(Role.REVISOR));
        assertFalse(user.hasRole(Role.ADMINISTRADOR));
    }

    @Test
    void canBeDeactivatedAndActivated() {
        User user = new User(1L, "a", "A", "a@x.co", Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "h");
        user.deactivate();
        assertFalse(user.isActive());
        user.activate();
        assertTrue(user.isActive());
    }

    @Test
    void requiresLoginAndRole() {
        assertThrows(IllegalArgumentException.class,
                () -> new User(null, " ", "A", "a@x.co", Role.REVISOR, null, "h"));
        assertThrows(NullPointerException.class,
                () -> new User(null, "a", "A", "a@x.co", null, null, "h"));
    }

    @Test
    void roleHasReadableLabel() {
        assertEquals("Revisor", Role.REVISOR.getLabel());
        assertEquals("Autor de preguntas", Role.AUTOR_PREGUNTAS.getLabel());
    }
}
