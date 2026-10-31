package co.unicauca.saberpro.users.application;

import co.unicauca.saberpro.users.application.port.PasswordHasherPort;
import co.unicauca.saberpro.users.application.port.UserRepositoryPort;
import co.unicauca.saberpro.users.domain.exception.UserDomainException;
import co.unicauca.saberpro.users.domain.exception.UserDomainException.Kind;
import co.unicauca.saberpro.users.domain.model.Role;
import co.unicauca.saberpro.users.domain.model.User;
import co.unicauca.saberpro.users.domain.model.UserStatus;
import co.unicauca.saberpro.users.domain.policy.PasswordPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * Casos de uso de Gestion de Usuarios: registro, autenticacion y consultas
 * (otros contextos consultan revisores, administradores y correos).
 * DIP: depende solo de abstracciones inyectadas por constructor.
 */
public class UserService {

    private final UserRepositoryPort repository;
    private final PasswordPolicy passwordPolicy;
    private final PasswordHasherPort passwordHasher;

    public UserService(UserRepositoryPort repository, PasswordPolicy passwordPolicy,
                       PasswordHasherPort passwordHasher) {
        this.repository = repository;
        this.passwordPolicy = passwordPolicy;
        this.passwordHasher = passwordHasher;
    }

    public User register(String login, String fullName, String email, Role role, String plainPassword) {
        List<String> errors = new ArrayList<>();
        if (isBlank(login)) errors.add("El login es obligatorio.");
        if (isBlank(fullName)) errors.add("El nombre completo es obligatorio.");
        if (isBlank(email) || !email.contains("@")) errors.add("El correo electrónico no es válido.");
        if (role == null) errors.add("Debe seleccionar un rol.");
        errors.addAll(passwordPolicy.validate(plainPassword));
        if (!errors.isEmpty()) {
            throw new UserDomainException(Kind.VALIDATION, "Datos de registro inválidos.", errors);
        }
        if (repository.existsByLogin(login.trim())) {
            throw new UserDomainException(Kind.DUPLICATED, "Ya existe un usuario con el login " + login + ".");
        }
        User user = new User(null, login, fullName.trim(), email.trim(), role, UserStatus.ACTIVO,
                passwordHasher.hash(plainPassword));
        return repository.save(user);
    }

    public User login(String login, String plainPassword) {
        User user = repository.findByLogin(login == null ? "" : login.trim())
                .filter(User::isActive)
                .filter(u -> passwordHasher.verify(plainPassword == null ? "" : plainPassword, u.getPasswordHash()))
                .orElseThrow(() -> new UserDomainException(Kind.INVALID_CREDENTIALS,
                        "Usuario o contraseña incorrectos, o la cuenta está inactiva."));
        return user;
    }

    public User findByLogin(String login) {
        return repository.findByLogin(login)
                .orElseThrow(() -> new UserDomainException(Kind.NOT_FOUND, "No existe el usuario " + login + "."));
    }

    public List<User> list(Role role) {
        return role == null ? repository.findAll() : repository.findByRole(role);
    }

    /** Usado por la carga inicial: crea el usuario solo si no existe. */
    public void registerIfMissing(String login, String fullName, String email, Role role, String plainPassword) {
        if (!repository.existsByLogin(login)) {
            register(login, fullName, email, role, plainPassword);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
