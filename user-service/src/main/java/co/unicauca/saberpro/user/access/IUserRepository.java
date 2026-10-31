package co.unicauca.saberpro.user.access;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.user.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Abstraccion (puerto) para la persistencia de usuarios.
 *
 * Principio de Inversion de Dependencias (DIP): UserService depende de esta
 * abstraccion y no de una implementacion concreta (SQLite, memoria, etc.).
 */
public interface IUserRepository {

    boolean save(User newUser);

    boolean update(User user);

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    List<User> list();

    List<User> listByRole(Role role);
}
