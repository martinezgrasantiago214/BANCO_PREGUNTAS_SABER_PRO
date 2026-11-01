package co.unicauca.saberpro.user.access;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.user.domain.UserStatus;
import co.unicauca.saberpro.user.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Crea, solo si no existen, un administrador, un autor y dos revisores de
 * ejemplo para poder probar de inmediato el sistema.
 *
 * Credenciales de prueba (usuario / clave):
 *   admin   / Admin123!
 *   autor1  / Autor123!
 *   rev1    / Revisor123!
 *   rev2    / Revisor123!
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    public DataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        createIfMissing("admin", "Administrador del sistema", "admin@unicauca.edu.co",
                Role.ADMINISTRADOR, "Admin123!");
        createIfMissing("autor1", "Docente Autor de Preguntas", "autor1@unicauca.edu.co",
                Role.AUTOR_PREGUNTAS, "Autor123!");
        createIfMissing("rev1", "Docente Revisor Uno", "rev1@unicauca.edu.co",
                Role.REVISOR, "Revisor123!");
        createIfMissing("rev2", "Docente Revisor Dos", "rev2@unicauca.edu.co",
                Role.REVISOR, "Revisor123!");
    }

    private void createIfMissing(String login, String fullName, String email, Role role, String password) {
        if (userService.findByLogin(login).isEmpty()) {
            userService.register(login, fullName, email, role, UserStatus.ACTIVO, password);
        }
    }
}
