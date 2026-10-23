package co.unicauca.saberpro.question.access;

import java.util.List;

/** Resultado paginado (HU03): la pagina pedida y el total, para que el cliente sepa cuantas paginas hay. */
public record PagedResult<T>(List<T> items, int page, int pageSize, long totalItems) {

    public int totalPages() {
        return (int) Math.max(1, Math.ceil((double) totalItems / pageSize));
    }
}
