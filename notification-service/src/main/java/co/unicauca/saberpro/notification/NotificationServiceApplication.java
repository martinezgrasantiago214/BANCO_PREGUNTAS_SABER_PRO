package co.unicauca.saberpro.notification;

import co.unicauca.saberpro.common.messaging.MessagingConfig;
import co.unicauca.saberpro.common.web.GlobalApiExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** Microservicio de notificaciones por correo, dirigido por eventos (puerto 8084). */
@SpringBootApplication
@Import({MessagingConfig.class, GlobalApiExceptionHandler.class})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
