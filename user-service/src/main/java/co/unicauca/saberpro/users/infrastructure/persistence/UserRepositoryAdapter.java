package co.unicauca.saberpro.users.infrastructure.persistence;

import co.unicauca.saberpro.users.application.port.UserRepositoryPort;
import co.unicauca.saberpro.users.domain.model.Role;
import co.unicauca.saberpro.users.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Adaptador de salida: implementa el puerto de dominio con Spring Data JPA. */
@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        UserEntity saved = jpa.save(new UserEntity(user.getId(), user.getLogin(), user.getFullName(),
                user.getEmail(), user.getRole(), user.getStatus(), user.getPasswordHash()));
        return toDomain(saved);
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return jpa.findByLogin(login).map(this::toDomain);
    }

    @Override
    public boolean existsByLogin(String login) {
        return jpa.existsByLogin(login);
    }

    @Override
    public List<User> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<User> findByRole(Role role) {
        return jpa.findByRoleOrderByFullNameAsc(role).stream().map(this::toDomain).toList();
    }

    private User toDomain(UserEntity e) {
        return new User(e.getId(), e.getLogin(), e.getFullName(), e.getEmail(), e.getRole(),
                e.getStatus(), e.getPasswordHash());
    }
}
