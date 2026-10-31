package co.unicauca.saberpro.users.domain.model;

import java.util.Objects;

/**
 * Entidad de dominio Usuario. SRP: solo conoce sus datos y sus reglas
 * (por ejemplo, si puede iniciar sesion). No sabe persistirse ni cifrar
 * contrasenas: eso vive en los puertos de salida.
 */
public class User {

    private Long id;
    private final String login;
    private String fullName;
    private String email;
    private Role role;
    private UserStatus status;
    private String passwordHash;

    public User(Long id, String login, String fullName, String email, Role role,
                UserStatus status, String passwordHash) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("El login es obligatorio.");
        }
        this.id = id;
        this.login = login.trim();
        this.fullName = fullName;
        this.email = email;
        this.role = Objects.requireNonNull(role, "El rol es obligatorio.");
        this.status = status == null ? UserStatus.ACTIVO : status;
        this.passwordHash = passwordHash;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVO;
    }

    public boolean hasRole(Role expected) {
        return role == expected;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVO;
    }

    public void activate() {
        this.status = UserStatus.ACTIVO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLogin() { return login; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public String getPasswordHash() { return passwordHash; }
}
