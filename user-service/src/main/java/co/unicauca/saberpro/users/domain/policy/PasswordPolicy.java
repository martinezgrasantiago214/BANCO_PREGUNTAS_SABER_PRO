package co.unicauca.saberpro.users.domain.policy;

import java.util.List;

/**
 * Patron Strategy: politica de contrasenas intercambiable (OCP). Devuelve la
 * lista de reglas incumplidas; lista vacia = contrasena valida.
 */
public interface PasswordPolicy {

    List<String> validate(String plainPassword);
}
