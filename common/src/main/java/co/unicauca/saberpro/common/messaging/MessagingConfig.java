package co.unicauca.saberpro.common.messaging;

import co.unicauca.saberpro.common.events.EventTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de mensajeria comun a todos los servicios: declara el
 * exchange de eventos y serializa los eventos como JSON. Spring Boot usa el
 * MessageConverter declarado aqui tanto en el RabbitTemplate como en los
 * @RabbitListener. Cada servicio la importa con @Import.
 */
@Configuration
public class MessagingConfig {

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EventTopics.EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter eventMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("co.unicauca.saberpro.common.events");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
