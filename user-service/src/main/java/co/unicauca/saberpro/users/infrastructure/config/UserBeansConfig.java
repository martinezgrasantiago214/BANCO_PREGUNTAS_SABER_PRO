package co.unicauca.saberpro.users.infrastructure.config;

import co.unicauca.saberpro.users.application.UserService;
import co.unicauca.saberpro.users.application.port.PasswordHasherPort;
import co.unicauca.saberpro.users.application.port.UserRepositoryPort;
import co.unicauca.saberpro.users.domain.policy.DefaultPasswordPolicy;
import co.unicauca.saberpro.users.domain.policy.PasswordPolicy;
import co.unicauca.saberpro.users.infrastructure.security.Pbkdf2PasswordHasher;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Composicion de dependencias: aqui se "enchufan" los adaptadores a los puertos. */
@Configuration
public class UserBeansConfig {

    @Bean
    PasswordPolicy passwordPolicy() {
        return new DefaultPasswordPolicy();
    }

    @Bean
    PasswordHasherPort passwordHasher() {
        return new Pbkdf2PasswordHasher();
    }

    @Bean
    UserService userService(UserRepositoryPort repository, PasswordPolicy policy, PasswordHasherPort hasher) {
        return new UserService(repository, policy, hasher);
    }

    @Bean
    OpenAPI userServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("user-service").version("2.0.0")
                        .description("Contexto Gestión de Usuarios: registro, login y consulta de roles."))
                .servers(List.of(new Server().url("/").description("Vía API Gateway")));
    }
}
