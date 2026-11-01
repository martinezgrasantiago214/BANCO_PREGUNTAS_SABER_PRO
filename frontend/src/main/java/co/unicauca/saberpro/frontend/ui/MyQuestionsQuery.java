package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.frontend.model.Models.PageView;

/**
 * Estado de la consulta de la pantalla "Mis preguntas" (HU03): filtros,
 * busqueda y pagina actual. No depende de Swing, asi la logica de filtros y
 * paginacion se prueba sin abrir ventanas.
 *
 * Cada cambio de filtro o de tamano de pagina vuelve a la pagina 1, para no
 * quedar en una pagina que ya no existe con el nuevo resultado.
 */
public class MyQuestionsQuery {

    public static final int[] PAGE_SIZES = {5, 10, 20};
    public static final int DEFAULT_PAGE_SIZE = 10;

    private QuestionStatus status;
    private String topic;
    private String keyword;
    private int page = 1;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private int totalPages = 1;
    private long totalItems;

    /** Aplica filtros nuevos (null o en blanco = sin filtro) y vuelve a la primera pagina. */
    public void filter(QuestionStatus status, String topic, String keyword) {
        this.status = status;
        this.topic = clean(topic);
        this.keyword = clean(keyword);
        this.page = 1;
    }

    /** Quita todos los filtros y vuelve a la primera pagina. */
    public void clearFilters() {
        filter(null, null, null);
    }

    public void changePageSize(int pageSize) {
        this.pageSize = Math.max(1, pageSize);
        this.page = 1;
    }

    /** @return true si habia pagina siguiente y se avanzo. */
    public boolean next() {
        if (!hasNext()) {
            return false;
        }
        page++;
        return true;
    }

    /** @return true si habia pagina anterior y se retrocedio. */
    public boolean previous() {
        if (!hasPrevious()) {
            return false;
        }
        page--;
        return true;
    }

    /** Toma del resultado del backend la pagina real y los totales. */
    public void update(PageView<?> result) {
        this.page = Math.max(1, result.page());
        this.totalPages = Math.max(1, result.totalPages());
        this.totalItems = result.totalItems();
    }

    public boolean hasNext() {
        return page < totalPages;
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasFilters() {
        return status != null || topic != null || keyword != null;
    }

    /** Texto del pie de la tabla, p. ej. "Pagina 2 de 3 (25 preguntas)". */
    public String summary() {
        return String.format("Pagina %d de %d (%d %s)", page, totalPages, totalItems,
                totalItems == 1 ? "pregunta" : "preguntas");
    }

    public QuestionStatus status() { return status; }
    public String topic() { return topic; }
    public String keyword() { return keyword; }
    public int page() { return page; }
    public int pageSize() { return pageSize; }
    public int totalPages() { return totalPages; }
    public long totalItems() { return totalItems; }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
