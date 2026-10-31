package co.unicauca.saberpro.question.domain.factory;

import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.QuestionRequest;
import co.unicauca.saberpro.question.domain.validation.QuestionPipeline;

/**
 * Patron Factory Method (GoF, creacional). Esta clase define el algoritmo
 * comun para crear una pregunta a partir de lo que capturo el autor, y deja
 * a cada subclase dos "metodos fabrica":
 *  - {@link #createQuestion(QuestionRequest)}: que pregunta concreta se construye.
 *  - {@link #createValidationPipeline()}: que validacion estructural le aplica.
 *
 * El codigo cliente (el servicio de aplicacion) trabaja solo con
 * QuestionFactory; agregar un tipo nuevo de pregunta es crear otra subclase
 * y registrarla en question-factories.properties (OCP).
 */
public abstract class QuestionFactory {

    /** Tipo de pregunta que produce esta fabrica, p. ej. MULTIPLE_CHOICE. */
    public abstract String getType();

    /** Nombre legible del tipo (se muestra en el API). */
    public abstract String getName();

    /** Factory method: construye la pregunta concreta de este tipo. */
    protected abstract Question createQuestion(QuestionRequest request);

    /** Factory method: pipeline de validacion estructural de este tipo. */
    public abstract QuestionPipeline createValidationPipeline();

    public boolean supports(String type) {
        return getType().equalsIgnoreCase(type);
    }

    /**
     * Valida la solicitud con el pipeline de este tipo y, si es valida,
     * construye la pregunta (que nace en estado Borrador).
     *
     * @throws QuestionValidationException si la solicitud no supera la validacion.
     */
    public final Question create(QuestionRequest request) {
        QuestionPipeline pipeline = createValidationPipeline();
        if (!pipeline.execute(request)) {
            throw new QuestionValidationException(pipeline.getLastError());
        }
        return createQuestion(request);
    }

    /** Builder ya cargado con los datos del autor y el tipo de esta fabrica. */
    protected Question.Builder baseBuilder(QuestionRequest request) {
        return Question.builder().type(getType()).fromRequest(request);
    }
}
