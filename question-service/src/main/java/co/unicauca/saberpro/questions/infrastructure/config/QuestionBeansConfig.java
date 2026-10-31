package co.unicauca.saberpro.questions.infrastructure.config;

import co.unicauca.saberpro.questions.application.QuestionService;
import co.unicauca.saberpro.questions.application.port.QuestionEventPublisherPort;
import co.unicauca.saberpro.questions.application.port.QuestionRepositoryPort;
import co.unicauca.saberpro.questions.domain.plugin.QuestionTypeRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class QuestionBeansConfig {

    @Bean
    QuestionTypeRegistry questionTypeRegistry() {
        return QuestionTypeRegistry.fromClasspath("plugins.properties");
    }

    @Bean
    QuestionService questionService(QuestionRepositoryPort repository, QuestionEventPublisherPort publisher,
                                     QuestionTypeRegistry registry) {
        return new QuestionService(repository, publisher, registry);
    }

    @Bean
    OpenAPI questionServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("question-service").version("2.0.0")
                        .description("Contextos Gestión de Preguntas, Ciclo de Vida y Validación Estructural."))
                .servers(List.of(new Server().url("/").description("Vía API Gateway")));
    }
}
