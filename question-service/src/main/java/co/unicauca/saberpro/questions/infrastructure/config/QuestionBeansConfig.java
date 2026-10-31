package co.unicauca.saberpro.questions.infrastructure.config;

import co.unicauca.saberpro.questions.application.QuestionService;
import co.unicauca.saberpro.questions.application.port.QuestionEventPublisherPort;
import co.unicauca.saberpro.questions.application.port.QuestionRepositoryPort;
import co.unicauca.saberpro.questions.domain.plugin.QuestionTypeRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import co.unicauca.saberpro.outbox.UseCaseTransactions;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.TransactionManager;

import java.util.List;

@Configuration
public class QuestionBeansConfig {

    /**
     * Frontera transaccional de los casos de uso: el agregado y sus eventos en
     * la outbox se confirman juntos. {@code QuestionService} no conoce Spring.
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor useCaseTransactions(TransactionManager transactionManager) {
        return UseCaseTransactions.advisor(transactionManager, QuestionService.class);
    }

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
