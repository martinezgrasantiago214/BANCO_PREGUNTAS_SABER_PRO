package co.unicauca.saberpro.access;

import co.unicauca.saberpro.domain.Role;
import co.unicauca.saberpro.domain.User;
import java.util.List;
import java.util.Optional;

/**
 * Abstraccion (puerto) para la persistencia de usuarios.
 *
 * Principio de Inversion de Dependencias (DIP): las capas de mas alto nivel
 * (UserService) dependen de esta abstraccion y no de una implementacion
 * concreta (SQLite, memoria, etc.).
 */
public interface IUserRepository {

    boolean save(User newUser);

    boolean update(User user);

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    List<User> list();

    List<User> listByRole(Role role);
}