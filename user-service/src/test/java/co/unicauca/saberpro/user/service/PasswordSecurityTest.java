package co.unicauca.saberpro.user.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de la politica de contrasenas y del cifrado PBKDF2. */
class PasswordSecurityTest {

    private final IPasswordPolicy policy = new DefaultPasswordPolicy();
    private final IPasswordHasher hasher = new PBKDF2PasswordHasher();

    @Test
    void aceptaUnaContrasenaQueCumpleTodasLasReglas() {
        assertTrue(policy.validate("Autor123!").isEmpty());
    }

    @Test
    void rechazaContrasenaCortaONula() {
        assertFalse(policy.validate("A1!").isEmpty());
        assertFalse(policy.validate(null).isEmpty());
    }

    @Test
    void exigeDigitoMayusculaYCaracterEspecial() {
        assertEquals(3, policy.validate("abcdefgh").size());
        assertEquals(1, policy.validate("Abcdefg!").size()); // falta digito
        assertEquals(1, policy.validate("abcdef1!").size()); // falta mayuscula
        assertEquals(1, policy.validate("Abcdef12").size()); // falta especial
    }

    @Test
    void elHashNoContieneLaContrasenaYSeVerifica() {
        String hash = hasher.hash("Autor123!");
        assertFalse(hash.contains("Autor123!"));
        assertTrue(hasher.verify("Autor123!", hash));
        assertFalse(hasher.verify("otraClave1!", hash));
    }

    @Test
    void dosHashesDeLaMismaContrasenaSonDistintosPorLaSal() {
        assertNotEquals(hasher.hash("Autor123!"), hasher.hash("Autor123!"));
    }

    @Test
    void unHashMalFormadoNoSeVerifica() {
        assertFalse(hasher.verify("Autor123!", "no-es-un-hash"));
    }
}
