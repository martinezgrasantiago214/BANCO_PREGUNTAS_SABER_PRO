package co.unicauca.saberpro.question.domain.validation;

import co.unicauca.saberpro.question.domain.QuestionRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Ejecuta en orden los filtros de validacion estructural. Se detiene en el
 * primer filtro que falla y expone su mensaje de error.
 */
public class QuestionPipeline {

    private final List<QuestionFilter> filters = new ArrayList<>();
    private String lastError;

    public QuestionPipeline addFilter(QuestionFilter filter) {
        filters.add(filter);
        return this;
    }

    public boolean execute(QuestionRequest request) {
        for (QuestionFilter filter : filters) {
            if (!filter.process(request)) {
                lastError = filter.getErrorMessage();
                return false;
            }
        }
        lastError = null;
        return true;
    }

    /**
     * Ejecuta TODOS los filtros, sin detenerse en el primero que falla, y
     * devuelve los mensajes de error de los que fallaron (lista vacia si la
     * solicitud es valida). Asi el autor ve de una vez todo lo que debe
     * corregir (PSP-17).
     */
    public List<String> validateAll(QuestionRequest request) {
        List<String> errors = new ArrayList<>();
        for (QuestionFilter filter : filters) {
            if (!filter.process(request)) {
                errors.add(filter.getErrorMessage());
            }
        }
        lastError = errors.isEmpty() ? null : errors.get(0);
        return errors;
    }

    public String getLastError() {
        return lastError;
    }

    public int size() {
        return filters.size();
    }
}
