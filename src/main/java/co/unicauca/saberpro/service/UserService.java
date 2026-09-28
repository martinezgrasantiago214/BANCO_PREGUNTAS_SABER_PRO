package co.unicauca.saberpro.service;

import co.unicauca.saberpro.access.IUserRepository;
import co.unicauca.saberpro.domain.Role;
import co.unicauca.saberpro.domain.User;
import co.unicauca.saberpro.domain.UserStatus;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de dominio con la logica de negocio de "Gestion de usuarios":
 * registro, autenticacion y consulta de revisores.
 *
 * Principio de Inversion de Dependencias (DIP): UserService no depende de
 * clases concretas, sino de tres abstracciones (IUserRepository,
 * IPasswordPolicy, IPasswordHasher) inyectadas por constructor.
 */
public class UserService {

    private final IUserRepository repository;
    private final IPasswordPolicy passwordPolicy;
    private final IPasswordHasher passwordHasher;

    public UserService(IUserRepository repository, IPasswordPolicy passwordPolicy,
                        IPasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordPolicy = passwordPolicy;
        this.passwordHasher = passwordHasher;
    }

    public OperationResult register(String login, String fullName, String email, Role role,
                                     UserStatus status, String plainPassword) {

        if (login == null || login.isBlank()) {
            return OperationResult.fail("El nombre de usuario (login) es obligatorio.");
        }
        if (fullName == null || fullName.isBlank()) {
            return OperationResult.fail("El nombre completo es obligatorio.");
        }
        if (role == null) {
            return OperationResult.fail("Debe seleccionar un rol.");
        }
        if (status == null) {
            return OperationResult.fail("Debe seleccionar un estado.");
        }

        List<String> passwordErrors = passwordPolicy.validate(plainPassword);
        if (!passwordErrors.isEmpty()) {
            return OperationResult.fail(passwordErrors);
        }

        if (repository.existsByLogin(login)) {
            return OperationResult.fail("Ya existe un usuario registrado con ese login.");
        }

        String hashed = passwordHasher.hash(plainPassword);
        User newUser = new User(login, fullName, email, role, status, hashed);

        boolean saved = repository.save(newUser);
        return saved ? OperationResult.ok()
                : OperationResult.fail("No fue posible guardar el usuario en la base de datos.");
    }

    public Optional<User> login(String login, String plainPassword) {
        Optional<User> found = repository.findByLogin(login);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        User user = found.get();
        if (!user.isActive()) {
            return Optional.empty();
        }
        if (!passwordHasher.verify(plainPassword, user.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public List<User> listUsers() {
        return repository.list();
    }

    /** Usado por HU04: el administrador solo puede asignar usuarios con rol REVISOR. */
    public List<User> listReviewers() {
        return repository.listByRole(Role.REVISOR);
    }

    public OperationResult updateUser(User user) {
        if (user == null) {
            return OperationResult.fail("El usuario es obligatorio.");
        }
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            return OperationResult.fail("El nombre de usuario (login) es obligatorio.");
        }
        if (user.getFullName() == null || user.getFullName().isBlank()) {
            return OperationResult.fail("El nombre completo es obligatorio.");
        }
        if (user.getRole() == null) {
            return OperationResult.fail("Debe seleccionar un rol.");
        }
        if (user.getStatus() == null) {
            return OperationResult.fail("Debe seleccionar un estado.");
        }

        boolean updated = repository.update(user);
        return updated ? OperationResult.ok()
                : OperationResult.fail("No fue posible actualizar el usuario.");
    }
}
