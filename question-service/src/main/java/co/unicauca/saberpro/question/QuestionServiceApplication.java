package co.unicauca.saberpro.question;

import co.unicauca.saberpro.common.messaging.MessagingConfig;
import co.unicauca.saberpro.common.web.GlobalApiExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** Microservicio de preguntas: gestion, validacion estructural y ciclo de vida (puerto 8082). */
@SpringBootApplication
@Import({MessagingConfig.class, GlobalApiExceptionHandler.class})
public class QuestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuestionServiceApplication.class, args);
    }
}
