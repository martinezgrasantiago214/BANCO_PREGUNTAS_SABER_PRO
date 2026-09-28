package co.unicauca.saberpro.api.config;

import co.unicauca.saberpro.access.AppPaths;
import co.unicauca.saberpro.access.DataInitializer;
import co.unicauca.saberpro.access.IQuestionRepository;
import co.unicauca.saberpro.access.IUserRepository;
import co.unicauca.saberpro.access.QuestionRepositoryFactory;
import co.unicauca.saberpro.access.UserRepositoryFactory;
import co.unicauca.saberpro.microkernel.core.QuestionMicrokernel;
import co.unicauca.saberpro.service.DefaultPasswordPolicy;
import co.unicauca.saberpro.service.IEmailService;
import co.unicauca.saberpro.service.PBKDF2PasswordHasher;
import co.unicauca.saberpro.service.QuestionService;
import co.unicauca.saberpro.service.SmtpEmailService;
import co.unicauca.saberpro.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensambla, como beans de Spring, las MISMAS piezas que arma AppContext en
 * la aplicacion de escritorio: repositorios SQLite (via sus fabricas),
 * microkernel con plugins cargados por Reflexion, servicio de correo y
 * servicios de dominio. Asi la API no duplica logica de negocio: solo
 * expone por HTTP lo que ya existe (inversion de dependencias).
 */
@Configuration
public class ApiConfig {

    @Bean
    public IUserRepository userRepository() {
        System.out.println("[API] Base de datos: " + AppPaths.getDatabaseUrl());
        IUserRepository repository = UserRepositoryFactory.getInstance().getRepository("file");
        DataInitializer.seed(repository);
        return repository;
    }

    @Bean
    public UserService userService(IUserRepository userRepository) {
        return new UserService(userRepository, new DefaultPasswordPolicy(), new PBKDF2PasswordHasher());
    }

    @Bean
    public IQuestionRepository questionRepository() {
        IQuestionRepository repository = QuestionRepositoryFactory.getInstance().getRepository("file");
        if (repository.getLastError() != null) {
            System.err.println("[API] Error inicializando la base de datos de preguntas: "
                    + repository.getLastError());
        }
        return repository;
    }

    @Bean
    public QuestionMicrokernel questionMicrokernel() {
        return new QuestionMicrokernel();
    }

    @Bean
    public IEmailService emailService() {
        return new SmtpEmailService();
    }

    @Bean
    public QuestionService questionService(IQuestionRepository questionRepository,
                                           QuestionMicrokernel questionMicrokernel,
                                           UserService userService,
                                           IEmailService emailService) {
        return new QuestionService(questionRepository, questionMicrokernel, userService, emailService);
    }
}
