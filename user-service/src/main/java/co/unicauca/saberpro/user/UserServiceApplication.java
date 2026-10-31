package co.unicauca.saberpro.user;

import co.unicauca.saberpro.common.web.GlobalApiExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** Microservicio del contexto "Gestion de Usuarios" (puerto 8081). */
@SpringBootApplication
@Import(GlobalApiExceptionHandler.class)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
