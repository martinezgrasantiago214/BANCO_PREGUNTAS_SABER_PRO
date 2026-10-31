package co.unicauca.saberpro.users.domain;

import co.unicauca.saberpro.users.domain.policy.DefaultPasswordPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPasswordPolicyTest {

    private final DefaultPasswordPolicy policy = new DefaultPasswordPolicy();

    @Test
    void acceptsStrongPassword() {
        assertTrue(policy.validate("Revisor123!").isEmpty());
    }

    @Test
    void rejectsShortOrNullPassword() {
        assertEquals(1, policy.validate("Ab1!").size());
        assertEquals(1, policy.validate(null).size());
    }

    @Test
    void reportsEachMissingRule() {
        List<String> errors = policy.validate("abcdefgh");
        assertEquals(3, errors.size(), "falta dígito, mayúscula y carácter especial");
    }
}
