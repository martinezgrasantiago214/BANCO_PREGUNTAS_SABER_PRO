package co.unicauca.saberpro.notifications.infrastructure.config;

import co.unicauca.saberpro.notifications.application.NotificationService;
import co.unicauca.saberpro.notifications.application.port.EmailSenderPort;
import co.unicauca.saberpro.notifications.application.port.NotificationRepositoryPort;
import co.unicauca.saberpro.notifications.application.port.UserDirectoryPort;
import co.unicauca.saberpro.notifications.application.template.*;
import co.unicauca.saberpro.notifications.infrastructure.email.OutboxFileEmailSenderAdapter;
import co.unicauca.saberpro.notifications.infrastructure.email.SmtpEmailSenderAdapter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.util.List;

@Configuration
public class NotificationBeansConfig {

    /**
     * Selecciona el adaptador de correo: notification.email.mode=smtp usa el
     * servidor SMTP configurado; outbox (por defecto) escribe archivos .txt.
     */
    @Bean
    EmailSenderPort emailSender(@Value("${notification.email.mode:outbox}") String mode,
                                @Value("${notification.email.outbox-dir:./outbox}") String outboxDir,
                                @Value("${notification.email.from:no-reply@saberpro.test}") String from,
                                @Value("${notification.email.simulated-latency-ms:0}") long latencyMs,
                                ObjectProvider<JavaMailSender> mailSender) {
        if ("smtp".equalsIgnoreCase(mode)) {
            return new SmtpEmailSenderAdapter(mailSender.getObject(), from);
        }
        return new OutboxFileEmailSenderAdapter(Path.of(outboxDir), latencyMs);
    }

    @Bean
    NotificationService notificationService(UserDirectoryPort directory, EmailSenderPort sender,
                                            NotificationRepositoryPort repository) {
        List<NotificationTemplate<?>> templates = List.of(
                new QuestionSubmittedTemplate(),
                new ReviewersAssignedTemplate(),
                new EvaluationSubmittedTemplate(),
                new ObservationAddedTemplate(),
                new ReviewCompletedTemplate());
        return new NotificationService(templates, directory, sender, repository);
    }

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    OpenAPI notificationServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("notification-service").version("2.0.0")
                        .description("Consumidor de eventos que notifica a los implicados por correo y bandeja."))
                .servers(List.of(new Server().url("/").description("Vía API Gateway")));
    }
}
