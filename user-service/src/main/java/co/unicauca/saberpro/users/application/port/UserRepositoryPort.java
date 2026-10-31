package co.unicauca.saberpro.users.application.port;

import co.unicauca.saberpro.users.domain.model.Role;
import co.unicauca.saberpro.users.domain.model.User;

import java.util.List;
import java.util.Optional;

/** Puerto de salida de persistencia (DIP: la aplicacion no conoce JPA). */
public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    List<User> findAll();

    List<User> findByRole(Role role);
}
