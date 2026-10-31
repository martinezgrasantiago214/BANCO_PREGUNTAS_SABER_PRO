package co.unicauca.saberpro.questions.domain.plugin;

import co.unicauca.saberpro.questions.domain.exception.QuestionDomainException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Nucleo del microkernel: registra los plugins de tipo de pregunta. Puede
 * cargarlos por Reflexion desde un archivo .properties (como en el primer
 * corte), de modo que agregar un tipo nuevo no modifica este codigo (OCP).
 */
public class QuestionTypeRegistry {

    private final List<QuestionTypePlugin> plugins;

    public QuestionTypeRegistry(List<QuestionTypePlugin> plugins) {
        this.plugins = List.copyOf(plugins);
    }

    public static QuestionTypeRegistry fromClasspath(String resource) {
        Properties props = new Properties();
        try (InputStream in = QuestionTypeRegistry.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró " + resource);
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + resource, e);
        }
        List<QuestionTypePlugin> loaded = new ArrayList<>();
        for (String key : new TreeSet<>(props.stringPropertyNames())) {
            String className = props.getProperty(key).trim();
            try {
                Object instance = Class.forName(className).getDeclaredConstructor().newInstance();
                loaded.add((QuestionTypePlugin) instance);
            } catch (ReflectiveOperationException | ClassCastException e) {
                throw new IllegalStateException("No se pudo cargar el plugin " + className, e);
            }
        }
        return new QuestionTypeRegistry(loaded);
    }

    public QuestionTypePlugin pluginFor(String type) {
        String requested = type == null || type.isBlank() ? MultipleChoiceQuestionPlugin.TYPE : type;
        return plugins.stream()
                .filter(p -> p.type().equalsIgnoreCase(requested))
                .findFirst()
                .orElseThrow(() -> new QuestionDomainException(QuestionDomainException.Kind.VALIDATION,
                        "Tipo de pregunta no soportado: " + requested));
    }

    public List<QuestionTypePlugin> plugins() {
        return Collections.unmodifiableList(plugins);
    }
}
