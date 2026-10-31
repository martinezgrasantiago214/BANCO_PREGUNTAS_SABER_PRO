package co.unicauca.saberpro.reviews.testdoubles;

import co.unicauca.saberpro.reviews.application.port.UserDirectoryPort;

import java.util.Map;

public class FakeUserDirectory implements UserDirectoryPort {

    private final Map<String, String> roles = Map.of(
            "admin", "ADMINISTRADOR",
            "autor1", "AUTOR_PREGUNTAS",
            "rev1", "REVISOR", "rev2", "REVISOR", "rev3", "REVISOR");

    @Override
    public boolean hasActiveRole(String login, String role) {
        return role.equals(roles.get(login));
    }
}
