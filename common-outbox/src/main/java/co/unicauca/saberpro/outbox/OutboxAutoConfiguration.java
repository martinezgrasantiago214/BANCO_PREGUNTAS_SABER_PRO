package co.unicauca.saberpro.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * Auto-configuracion del Transactional Outbox. Basta con agregar la
 * dependencia {@code common-outbox} a un servicio: la entidad y el
 * repositorio se registran junto a los del servicio gracias a
 * {@link AutoConfigurationPackage}, y el relay arranca solo.
 */
@AutoConfiguration(before = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@AutoConfigurationPackage(basePackageClasses = OutboxMessage.class)
@ConditionalOnClass({RabbitTemplate.class, OutboxRepository.class})
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock outboxClock() {
        return Clock.systemUTC();
    }

    @Bean
    OutboxEventWriter outboxEventWriter(OutboxRepository repository, ObjectMapper objectMapper, Clock clock) {
        return new OutboxEventWriter(repository, objectMapper, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxMessageSender outboxMessageSender(RabbitTemplate rabbitTemplate, OutboxProperties properties) {
        return new RabbitOutboxMessageSender(rabbitTemplate, properties.confirmTimeout());
    }

    @Bean
    OutboxRelay outboxRelay(OutboxRepository repository, OutboxMessageSender sender, ObjectMapper objectMapper,
                            PlatformTransactionManager transactionManager, Clock clock, OutboxProperties properties) {
        return new OutboxRelay(repository, sender, objectMapper, new TransactionTemplate(transactionManager),
                clock, properties.batchSize(), properties.pollInterval(), properties.maxBackoff());
    }

    @Bean
    @ConditionalOnProperty(prefix = "saberpro.outbox", name = "enabled", matchIfMissing = true)
    OutboxRelayScheduler outboxRelayScheduler(OutboxRelay relay, OutboxProperties properties) {
        return new OutboxRelayScheduler(relay, properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    static class OutboxHealthConfiguration {

        @Bean
        OutboxHealthIndicator outboxHealthIndicator(OutboxRepository repository, Clock clock,
                                                    OutboxProperties properties) {
            return new OutboxHealthIndicator(repository, clock, properties.maxPendingAge());
        }
    }
}
