package co.unicauca.saberpro.question.config;

import co.unicauca.saberpro.question.access.IQuestionRepository;
import co.unicauca.saberpro.question.access.SQLiteQuestionRepository;
import co.unicauca.saberpro.question.application.QuestionApplicationService;
import co.unicauca.saberpro.question.domain.factory.QuestionFactoryRegistry;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.EventLogListener;
import co.unicauca.saberpro.common.messaging.RabbitEventForwarder;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/** Ensambla el servicio: el dominio y el servicio de aplicacion no dependen de Spring. */
@Configuration
public class QuestionServiceConfig {

    @Bean
    public IQuestionRepository questionRepository(@Value("${saberpro.data-dir}") String dataDir) {
        File dir = new File(dataDir);
        dir.mkdirs();
        return new SQLiteQuestionRepository("jdbc:sqlite:" + new File(dir, "question-service.db").getAbsolutePath());
    }

    @Bean
    public QuestionFactoryRegistry questionFactoryRegistry() {
        return new QuestionFactoryRegistry();
    }

    /** Sujeto del patron Observer con sus dos observadores: el log y el reenvio al broker. */
    @Bean
    public DomainEventBus domainEventBus(AmqpTemplate amqpTemplate) {
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(new EventLogListener());
        bus.subscribe(new RabbitEventForwarder(amqpTemplate));
        return bus;
    }

    @Bean
    public QuestionApplicationService questionApplicationService(IQuestionRepository repository,
                                                                 QuestionFactoryRegistry factories,
                                                                 DomainEventBus eventBus) {
        return new QuestionApplicationService(repository, factories, eventBus);
    }
}
