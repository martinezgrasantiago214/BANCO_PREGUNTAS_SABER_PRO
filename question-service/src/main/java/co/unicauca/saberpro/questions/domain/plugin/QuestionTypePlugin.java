package co.unicauca.saberpro.questions.domain.plugin;

import co.unicauca.saberpro.questions.domain.exception.QuestionDomainException;
import co.unicauca.saberpro.questions.domain.model.Question;
import co.unicauca.saberpro.questions.domain.model.QuestionData;
import co.unicauca.saberpro.questions.domain.model.QuestionOption;
import co.unicauca.saberpro.questions.domain.validation.QuestionValidationChain;
import co.unicauca.saberpro.questions.domain.validation.QuestionValidationHandler;
import co.unicauca.saberpro.questions.domain.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Plugin del microkernel de tipos de pregunta (heredado del primer corte).
 *
 * Patron Factory Method (GoF, creacional): esta clase es el "Creator".
 * {@link #generate} define el algoritmo comun (validar y luego crear) y
 * delega en el metodo fabrica {@link #createQuestion}, que cada tipo de
 * pregunta concreto sobrescribe. El servicio de aplicacion nunca hace
 * "new" de una pregunta: pide al plugin adecuado que la fabrique.
 */
public abstract class QuestionTypePlugin {

    /** Codigo del tipo, p. ej. MULTIPLE_CHOICE. */
    public abstract String type();

    public abstract String displayName();

    /** Cadena de validacion de este tipo; los tipos pueden extenderla. */
    protected QuestionValidationHandler validationChain() {
        return QuestionValidationChain.standard();
    }

    /** Lanza VALIDATION con todos los errores si los datos no son validos. */
    public final void validate(QuestionData data) {
        ValidationResult result = validationChain().validate(data);
        if (!result.isValid()) {
            throw new QuestionDomainException(QuestionDomainException.Kind.VALIDATION,
                    "La pregunta no cumple la validación estructural.", result.getErrors());
        }
    }

    /** Template: valida y luego invoca el Factory Method. */
    public final Question generate(QuestionData data) {
        validate(data);
        return createQuestion(data);
    }

    /** Factory Method: cada tipo decide como construir su pregunta. */
    protected abstract Question createQuestion(QuestionData data);

    /** Convierte los textos de opciones en entidades Opcion marcando la correcta. */
    public List<QuestionOption> buildOptions(QuestionData data) {
        List<QuestionOption> result = new ArrayList<>();
        for (int i = 0; i < data.options().size(); i++) {
            String text = data.options().get(i);
            boolean correct = text.trim().equalsIgnoreCase(data.correctAnswer().trim());
            result.add(new QuestionOption(text, correct, i + 1));
        }
        return result;
    }
}
