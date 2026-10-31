package co.unicauca.saberpro.users.infrastructure.config;

import co.unicauca.saberpro.users.application.UserService;
import co.unicauca.saberpro.users.domain.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Usuarios de prueba (login / clave):
 * admin/Admin123!, autor1/Autor123!, autor2/Autor123!,
 * rev1/Revisor123!, rev2/Revisor123!, rev3/Revisor123!
 *
 * Para la demo con correo real: si se define app.seed.email-base (p. ej.
 * equipo@gmail.com), cada usuario recibe equipo+login@gmail.com, de modo que
 * todos los correos llegan a la misma bandeja de Gmail.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserService userService;
    private final String emailDomain;
    private final String emailBase;

    public DataSeeder(UserService userService,
                      @Value("${app.seed.email-domain:saberpro.test}") String emailDomain,
                      @Value("${app.seed.email-base:}") String emailBase) {
        this.userService = userService;
        this.emailDomain = emailDomain;
        this.emailBase = emailBase;
    }

    @Override
    public void run(String... args) {
        seed("admin", "Administrador del Banco", Role.ADMINISTRADOR, "Admin123!");
        seed("autor1", "Docente Autor Uno", Role.AUTOR_PREGUNTAS, "Autor123!");
        seed("autor2", "Docente Autor Dos", Role.AUTOR_PREGUNTAS, "Autor123!");
        seed("rev1", "Docente Revisor Uno", Role.REVISOR, "Revisor123!");
        seed("rev2", "Docente Revisor Dos", Role.REVISOR, "Revisor123!");
        seed("rev3", "Docente Revisor Tres", Role.REVISOR, "Revisor123!");
    }

    private void seed(String login, String name, Role role, String password) {
        userService.registerIfMissing(login, name, emailFor(login), role, password);
    }

    String emailFor(String login) {
        if (emailBase != null && emailBase.contains("@")) {
            String[] parts = emailBase.split("@", 2);
            return parts[0] + "+" + login + "@" + parts[1];
        }
        return login + "@" + emailDomain;
    }
}
