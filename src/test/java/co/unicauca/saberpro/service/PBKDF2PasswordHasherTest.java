package co.unicauca.saberpro.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del cifrado de contrasenas. */
class PBKDF2PasswordHasherTest {

    private final IPasswordHasher hasher = new PBKDF2PasswordHasher();

    @Test
    void elHashNoContieneLaContrasenaEnTextoPlano() {
        String hash = hasher.hash("Autor123!");
        assertNotNull(hash);
        assertFalse(hash.contains("Autor123!"));
    }

    @Test
    void verificaLaContrasenaCorrecta() {
        String hash = hasher.hash("Autor123!");
        assertTrue(hasher.verify("Autor123!", hash));
    }

    @Test
    void rechazaUnaContrasenaIncorrecta() {
        String hash = hasher.hash("Autor123!");
        assertFalse(hasher.verify("otraClave1!", hash));
    }

    @Test
    void dosHashesDeLaMismaContrasenaSonDistintosPorLaSal() {
        assertNotEquals(hasher.hash("Autor123!"), hasher.hash("Autor123!"));
    }
}
