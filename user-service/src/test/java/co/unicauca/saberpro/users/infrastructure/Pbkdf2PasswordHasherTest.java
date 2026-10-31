package co.unicauca.saberpro.users.infrastructure;

import co.unicauca.saberpro.users.infrastructure.security.Pbkdf2PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Pbkdf2PasswordHasherTest {

    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();

    @Test
    void verifiesCorrectPasswordOnly() {
        String hash = hasher.hash("Admin123!");
        assertTrue(hasher.verify("Admin123!", hash));
        assertFalse(hasher.verify("admin123!", hash));
    }

    @Test
    void usesRandomSalt() {
        assertNotEquals(hasher.hash("Admin123!"), hasher.hash("Admin123!"));
    }

    @Test
    void malformedHashIsRejected() {
        assertFalse(hasher.verify("x", "no-es-un-hash"));
    }
}
