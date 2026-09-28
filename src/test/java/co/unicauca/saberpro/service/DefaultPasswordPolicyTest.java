package co.unicauca.saberpro.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de la politica de contrasenas (Strategy / OCP). */
class DefaultPasswordPolicyTest {

    private final IPasswordPolicy policy = new DefaultPasswordPolicy();

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
}
