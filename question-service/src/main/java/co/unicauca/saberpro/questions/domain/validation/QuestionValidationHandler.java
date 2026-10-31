package co.unicauca.saberpro.questions.domain.validation;

import co.unicauca.saberpro.questions.domain.model.QuestionData;

import java.util.Optional;

/**
 * Patron Chain of Responsibility (GoF, comportamiento) para la validacion
 * estructural (HU03 del enunciado). Cada eslabon revisa UNA regla y pasa la
 * solicitud al siguiente. A diferencia del pipeline del primer corte, la
 * cadena no se detiene en el primer error: recoge todos para que el autor
 * los corrija de una sola vez.
 *
 * {@link #handle} es ademas un Template Method: fija el algoritmo (revisar,
 * registrar, delegar) y deja a las subclases solo el paso {@link #check}.
 */
public abstract class QuestionValidationHandler {

    private QuestionValidationHandler next;

    /** Enlaza el siguiente eslabon y lo devuelve para encadenar con fluidez. */
    public QuestionValidationHandler linkWith(QuestionValidationHandler next) {
        this.next = next;
        return next;
    }

    public final ValidationResult validate(QuestionData data) {
        ValidationResult result = new ValidationResult();
        handle(data, result);
        return result;
    }

    protected final void handle(QuestionData data, ValidationResult result) {
        check(data).ifPresent(result::addError);
        if (next != null) {
            next.handle(data, result);
        }
    }

    /** @return mensaje de error si la regla no se cumple. */
    protected abstract Optional<String> check(QuestionData data);

    protected static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
