package co.unicauca.saberpro.notifications.application.port;

import co.unicauca.saberpro.notifications.domain.Recipient;

import java.util.List;
import java.util.Optional;

/** Resuelve nombre y correo de los usuarios (los eventos solo traen logins). */
public interface UserDirectoryPort {

    Optional<Recipient> findByLogin(String login);

    List<Recipient> findByRole(String role);
}
