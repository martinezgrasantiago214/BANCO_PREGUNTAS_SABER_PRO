package co.unicauca.saberpro.microkernel.core;

import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.microkernel.common.QuestionPlugin;

import java.io.InputStream;
import java.util.*;

/**
 * Nucleo (Microkernel) del sistema de Banco de Preguntas.
 *
 * Responsabilidades (segun el taller):
 *  - Almacenar el banco de preguntas en un Map.
 *  - Registrar los plugins declarados en plugins.properties.
 *  - Cargar dinamicamente los plugins mediante Reflexion (obligatorio).
 *  - Ejecutar el plugin adecuado segun el tipo de pregunta solicitado
 *    (gestion del ciclo de vida / ejecucion de plugins).
 *
 * El nucleo NUNCA conoce las clases concretas de los plugins: solo conoce
 * la abstraccion QuestionPlugin (Principio de Inversion de Dependencias) y
 * los descubre en tiempo de ejecucion via Reflexion, igual que el ejemplo
 * de clase del envio de paquetes a distintos paises.
 */
public class QuestionMicrokernel {

    private static final String PLUGINS_CONFIG_FILE = "plugins.properties";

    private final Map<String, Question> questions = new HashMap<>();
    private final List<QuestionPlugin> plugins = new ArrayList<>();
    private String lastError;

    public QuestionMicrokernel() {
        loadPlugins();
    }

    /**
     * Lee plugins.properties y, mediante Reflexion, instancia cada plugin
     * registrado sin que el nucleo conozca sus clases concretas (DIP).
     */
    private void loadPlugins() {
        try {
            Properties prop = new Properties();
            InputStream input = getClass().getClassLoader().getResourceAsStream(PLUGINS_CONFIG_FILE);
            if (input == null) {
                System.err.println("No se encontro el archivo plugins.properties");
                return;
            }
            prop.load(input);

            List<String> keys = new ArrayList<>(prop.stringPropertyNames());
            Collections.sort(keys);
            for (String key : keys) {
                String className = prop.getProperty(key);
                try {
                    // Uso obligatorio de Reflexion para instanciacion dinamica
                    Class<?> clazz = Class.forName(className);
                    QuestionPlugin plugin = (QuestionPlugin) clazz.getDeclaredConstructor().newInstance();
                    plugins.add(plugin);
                    System.out.println("Plugin cargado via reflexion: " + plugin.getName() + " (" + className + ")");
                } catch (Exception e) {
                    System.err.println("Error al cargar el plugin '" + className + "': " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error al leer plugins.properties: " + e.getMessage());
        }
    }

    /**
     * Busca el plugin que soporte el tipo indicado y delega en el la
     * generacion (validacion estructural incluida, HU03) de la pregunta.
     * Si la validacion falla, retorna null y el motivo queda disponible en
     * getLastError().
     */
    public Question executePlugin(String type, QuestionRequest request) {
        for (QuestionPlugin plugin : plugins) {
            if (plugin.supports(type)) {
                Question question = plugin.generate(request);
                if (question != null) {
                    questions.put(question.getId(), question);
                    lastError = null;
                } else {
                    lastError = plugin.getLastValidationError();
                }
                return question;
            }
        }
        throw new IllegalArgumentException("No hay ningun plugin registrado que soporte el tipo: " + type);
    }

    public String getLastError() {
        return lastError;
    }

    public Map<String, Question> getQuestions() {
        return questions;
    }

    public List<QuestionPlugin> getPlugins() {
        return plugins;
    }
}
