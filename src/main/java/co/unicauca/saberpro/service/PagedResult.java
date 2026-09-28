package co.unicauca.saberpro.service;

import java.util.List;

/**
 * Resultado paginado generico, usado por HU03 (listar preguntas del autor
 * con paginacion y filtros) para que la interfaz grafica sepa cuantas
 * paginas hay disponibles sin cargar todo el banco de preguntas en memoria.
 */
public class PagedResult<T> {

    private final List<T> items;
    private final int page;       // pagina actual, 1-based
    private final int pageSize;
    private final long totalItems;

    public PagedResult(List<T> items, int page, int pageSize, long totalItems) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return (int) Math.max(1, Math.ceil((double) totalItems / pageSize));
    }
}
