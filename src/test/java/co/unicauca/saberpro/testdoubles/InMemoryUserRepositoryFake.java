package co.unicauca.saberpro.testdoubles;

import co.unicauca.saberpro.access.IUserRepository;
import co.unicauca.saberpro.domain.Role;
import co.unicauca.saberpro.domain.User;

import java.util.*;

/** Doble de prueba en memoria para IUserRepository, usado en las pruebas unitarias. */
public class InMemoryUserRepositoryFake implements IUserRepository {

    private final Map<String, User> users = new LinkedHashMap<>();
    private int nextId = 1;

    @Override
    public boolean save(User newUser) {
        newUser.setUserId(nextId++);
        users.put(newUser.getLogin(), newUser);
        return true;
    }

    @Override
    public boolean update(User user) {
        if (!users.containsKey(user.getLogin())) {
            return false;
        }
        users.put(user.getLogin(), user);
        return true;
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
    public List<User> list() {
        return new ArrayList<>(users.values());
    }

    @Override
    public List<User> listByRole(Role role) {
        List<User> result = new ArrayList<>();
        for (User u : users.values()) {
            if (u.getRole() == role) {
                result.add(u);
            }
        }
        return result;
    }
}
