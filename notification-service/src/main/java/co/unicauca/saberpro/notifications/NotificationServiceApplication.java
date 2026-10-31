package co.unicauca.saberpro.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de notificaciones: consume eventos y envia correos. Es
 * stateless respecto a los mensajes, por lo que se escala con N replicas que
 * compiten por la misma cola (competing consumers).
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
