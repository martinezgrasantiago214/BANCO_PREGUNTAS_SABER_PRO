package co.unicauca.saberpro.review;

import co.unicauca.saberpro.common.messaging.MessagingConfig;
import co.unicauca.saberpro.common.web.GlobalApiExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** Microservicio de revision por pares: asignacion de revisores y evaluaciones (puerto 8083). */
@SpringBootApplication
@Import({MessagingConfig.class, GlobalApiExceptionHandler.class})
public class ReviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewServiceApplication.class, args);
    }
}
