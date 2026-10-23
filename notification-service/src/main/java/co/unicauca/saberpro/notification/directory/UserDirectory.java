package co.unicauca.saberpro.notification.directory;

import co.unicauca.saberpro.notification.domain.Recipient;

import java.util.List;
import java.util.Optional;

/** Puerto para obtener los datos de contacto de los usuarios (los tiene el user-service). */
public interface UserDirectory {

    Optional<Recipient> findByLogin(String login);

    List<Recipient> findAdministrators();
}
