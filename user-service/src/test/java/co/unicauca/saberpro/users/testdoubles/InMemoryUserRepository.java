package co.unicauca.saberpro.users.testdoubles;

import co.unicauca.saberpro.users.application.port.UserRepositoryPort;
import co.unicauca.saberpro.users.domain.model.Role;
import co.unicauca.saberpro.users.domain.model.User;

import java.util.*;

/** Fake en memoria del puerto de persistencia (sin base de datos). */
public class InMemoryUserRepository implements UserRepositoryPort {

    private final Map<String, User> users = new LinkedHashMap<>();
    private long sequence = 1;

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(sequence++);
        }
        users.put(user.getLogin(), user);
        return user;
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return Optional.ofNullable(users.get(login));
    }

    @Override
    public boolean existsByLogin(String login) {
        return users.containsKey(login);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public List<User> findByRole(Role role) {
        return users.values().stream().filter(u -> u.getRole() == role).toList();
    }
}
