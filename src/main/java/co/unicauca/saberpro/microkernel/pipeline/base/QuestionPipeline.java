package co.unicauca.saberpro.microkernel.pipeline.base;

import co.unicauca.saberpro.domain.QuestionRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordina la ejecucion secuencial de los filtros de validacion
 * (patron Tuberias y Filtros). La tuberia se detiene en el primer filtro
 * que falle y expone el mensaje de error correspondiente.
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
                lastError = filter.getClass().getSimpleName() + ": " + filter.getErrorMessage();
                return false;
            }
        }
        lastError = null;
        return true;
    }

    public String getLastError() {
        return lastError;
    }
}
