package co.unicauca.saberpro.users.infrastructure.persistence;

import co.unicauca.saberpro.users.domain.model.Role;
import co.unicauca.saberpro.users.domain.model.UserStatus;
import jakarta.persistence.*;

/** Modelo de persistencia (separado de la entidad de dominio). */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String login;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(nullable = false, length = 200)
    private String passwordHash;

    protected UserEntity() {
    }

    public UserEntity(Long id, String login, String fullName, String email, Role role,
                      UserStatus status, String passwordHash) {
        this.id = id;
        this.login = login;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public String getPasswordHash() { return passwordHash; }
}
