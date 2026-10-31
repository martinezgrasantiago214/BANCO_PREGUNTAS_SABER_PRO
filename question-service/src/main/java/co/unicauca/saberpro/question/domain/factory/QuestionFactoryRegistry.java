package co.unicauca.saberpro.question.domain.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Registro de fabricas de preguntas (heredero del microkernel del primer
 * corte): lee question-factories.properties y crea cada fabrica por
 * Reflexion, sin conocer sus clases concretas (DIP).
 */
public class QuestionFactoryRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionFactoryRegistry.class);
    public static final String DEFAULT_CONFIG = "question-factories.properties";

    private final List<QuestionFactory> factories = new ArrayList<>();

    public QuestionFactoryRegistry() {
        this(DEFAULT_CONFIG);
    }

    public QuestionFactoryRegistry(String configResource) {
        load(configResource);
    }

    private void load(String configResource) {
        Properties config = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(configResource)) {
            if (in == null) {
                LOGGER.error("No se encontro {} en el classpath", configResource);
                return;
            }
            config.load(in);
        } catch (IOException ex) {
            LOGGER.error("No fue posible leer {}", configResource, ex);
            return;
        }

        List<String> keys = new ArrayList<>(config.stringPropertyNames());
        Collections.sort(keys);
        for (String key : keys) {
            String className = config.getProperty(key).trim();
            try {
                QuestionFactory factory = (QuestionFactory) Class.forName(className)
                        .getDeclaredConstructor().newInstance();
                factories.add(factory);
                LOGGER.info("Fabrica de preguntas cargada por reflexion: {} ({})", factory.getType(), className);
            } catch (ReflectiveOperationException | ClassCastException ex) {
                LOGGER.error("No fue posible cargar la fabrica {}", className, ex);
            }
        }
    }

    public Optional<QuestionFactory> forType(String type) {
        return factories.stream().filter(factory -> factory.supports(type)).findFirst();
    }

    public List<QuestionFactory> all() {
        return Collections.unmodifiableList(factories);
    }
}
