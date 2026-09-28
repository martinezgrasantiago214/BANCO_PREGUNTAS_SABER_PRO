package co.unicauca.saberpro.domain;

/**
 * Entidad de dominio que representa un usuario del sistema (autor de
 * preguntas, administrador o revisor).
 *
 * Principio de Responsabilidad Unica (SRP): esta clase SOLO conoce y expone
 * los datos de un usuario. No sabe como se valida ni se cifra una
 * contrasena, ni como se persiste; esas responsabilidades viven en otras
 * clases (IPasswordPolicy, IPasswordHasher, IUserRepository).
 */
public class User {

    private int userId;
    private String login;
    private String fullName;
    private String email;
    private Role role;
    private UserStatus status;

    /** Contrasena ya cifrada (hash + salt codificados). */
    private String passwordHash;

    public User() {
    }

    public User(String login, String fullName, String email, Role role, UserStatus status, String passwordHash) {
        this.login = login;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status;
        this.passwordHash = passwordHash;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVO;
    }

    @Override
    public String toString() {
        return fullName + " (" + login + ") - " + role;
    }
}
