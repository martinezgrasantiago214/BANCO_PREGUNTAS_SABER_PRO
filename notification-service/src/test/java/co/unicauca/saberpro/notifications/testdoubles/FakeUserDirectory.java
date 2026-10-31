package co.unicauca.saberpro.notifications.testdoubles;

import co.unicauca.saberpro.notifications.application.port.UserDirectoryPort;
import co.unicauca.saberpro.notifications.domain.Recipient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakeUserDirectory implements UserDirectoryPort {

    private final Map<String, Recipient> users = Map.of(
            "admin", new Recipient("admin", "Admin", "admin@x.co"),
            "autor1", new Recipient("autor1", "Autor Uno", "autor1@x.co"),
            "rev1", new Recipient("rev1", "Revisor Uno", "rev1@x.co"),
            "rev2", new Recipient("rev2", "Revisor Dos", "rev2@x.co"),
            "sincorreo", new Recipient("sincorreo", "Sin correo", ""));

    @Override
    public Optional<Recipient> findByLogin(String login) {
        return Optional.ofNullable(users.get(login));
    }

    @Override
    public List<Recipient> findByRole(String role) {
        return "ADMINISTRADOR".equals(role) ? List.of(users.get("admin")) : List.of();
    }
}
