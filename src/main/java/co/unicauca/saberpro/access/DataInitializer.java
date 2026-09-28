package co.unicauca.saberpro.access;

import co.unicauca.saberpro.domain.Role;
import co.unicauca.saberpro.domain.UserStatus;
import co.unicauca.saberpro.service.IPasswordHasher;
import co.unicauca.saberpro.service.PBKDF2PasswordHasher;
import co.unicauca.saberpro.domain.User;

/**
 * Crea, solo si no existen, un usuario administrador, un autor y dos
 * revisores de ejemplo, para poder probar de inmediato las 4 historias de
 * usuario sin tener que registrar usuarios manualmente.
 *
 * Credenciales de prueba (usuario / clave):
 *   admin   / Admin123!
 *   autor1  / Autor123!
 *   rev1    / Revisor123!
 *   rev2    / Revisor123!
 */
public final class DataInitializer {

    private DataInitializer() {
    }

    public static void seed(IUserRepository userRepository) {
        IPasswordHasher hasher = new PBKDF2PasswordHasher();

        createIfMissing(userRepository, hasher, "admin", "Administrador del sistema",
                "admin@unicauca.edu.co", Role.ADMINISTRADOR, "Admin123!");
        createIfMissing(userRepository, hasher, "autor1", "Docente Autor de Preguntas",
                "autor1@unicauca.edu.co", Role.AUTOR_PREGUNTAS, "Autor123!");
        createIfMissing(userRepository, hasher, "rev1", "Docente Revisor Uno",
                "rev1@unicauca.edu.co", Role.REVISOR, "Revisor123!");
        createIfMissing(userRepository, hasher, "rev2", "Docente Revisor Dos",
                "rev2@unicauca.edu.co", Role.REVISOR, "Revisor123!");
    }

    private static void createIfMissing(IUserRepository repo, IPasswordHasher hasher, String login,
                                         String fullName, String email, Role role, String plainPassword) {
        if (repo.existsByLogin(login)) {
            return;
        }
        User user = new User(login, fullName, email, role, UserStatus.ACTIVO, hasher.hash(plainPassword));
        repo.save(user);
    }
}
