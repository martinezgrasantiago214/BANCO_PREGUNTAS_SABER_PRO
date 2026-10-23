package co.unicauca.saberpro.notification.config;

import co.unicauca.saberpro.notification.application.NotificationService;
import co.unicauca.saberpro.notification.channel.OutboxFileChannel;
import co.unicauca.saberpro.notification.channel.SmtpEmailChannelAdapter;
import co.unicauca.saberpro.notification.channel.SmtpMailClient;
import co.unicauca.saberpro.notification.directory.UserDirectory;
import co.unicauca.saberpro.notification.directory.UserServiceRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/** Ensambla el notification-service. */
@Configuration
public class NotificationServiceConfig {

    @Bean
    public UserDirectory userDirectory(@Value("${saberpro.user-service-url}") String userServiceUrl) {
        return new UserServiceRestClient(userServiceUrl);
    }

    @Bean
    public NotificationService notificationService(UserDirectory userDirectory,
                                                   @Value("${saberpro.outbox-dir}") String outboxDir) {
        return new NotificationService(new SmtpEmailChannelAdapter(SmtpMailClient.fromClasspath()),
                new OutboxFileChannel(new File(outboxDir)), userDirectory);
    }
}
