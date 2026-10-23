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

    public String getLastError() {
        return lastError;
    }

    public int size() {
        return filters.size();
    }
}
