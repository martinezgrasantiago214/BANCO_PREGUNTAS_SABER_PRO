package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.frontend.model.Models.PageView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HU03 (PSP-29): logica de busqueda, filtros y paginacion de la pantalla "Mis preguntas". */
class MyQuestionsQueryTest {

    private final MyQuestionsQuery query = new MyQuestionsQuery();

    private static PageView<String> page(int page, int totalPages, long totalItems) {
        return new PageView<>(List.of(), page, 10, totalItems, totalPages);
    }

    @Test
    void empiezaEnLaPrimeraPaginaSinFiltros() {
        assertEquals(1, query.page());
        assertEquals(MyQuestionsQuery.DEFAULT_PAGE_SIZE, query.pageSize());
        assertFalse(query.hasFilters());
        assertFalse(query.hasPrevious());
        assertFalse(query.hasNext());
    }

    @Test
    void avanzaYRetrocedeSoloDentroDelRango() {
        query.update(page(1, 3, 25));

        assertTrue(query.next());
        assertTrue(query.next());
        assertEquals(3, query.page());
        assertFalse(query.next());

        assertTrue(query.previous());
        assertEquals(2, query.page());
        query.previous();
        assertFalse(query.previous());
        assertEquals(1, query.page());
    }

    @Test
    void aplicarFiltrosVuelveALaPrimeraPaginaYLimpiaLosTextos() {
        query.update(page(3, 3, 25));

        query.filter(QuestionStatus.BORRADOR, "  Arquitectura ", "   ");

        assertEquals(1, query.page());
        assertEquals(QuestionStatus.BORRADOR, query.status());
        assertEquals("Arquitectura", query.topic());
        assertNull(query.keyword());
        assertTrue(query.hasFilters());
    }

    @Test
    void limpiarFiltrosQuitaTodos() {
        query.filter(QuestionStatus.APROBADA, "Tema", "palabra");
        query.clearFilters();

        assertFalse(query.hasFilters());
        assertNull(query.status());
    }

    @Test
    void cambiarElTamanoDePaginaVuelveALaPrimera() {
        query.update(page(2, 3, 25));
        query.changePageSize(20);

        assertEquals(20, query.pageSize());
        assertEquals(1, query.page());
    }

    @Test
    void tomaLaPaginaRealQueDevuelveElBackend() {
        // El backend devuelve la ultima pagina si la pedida ya no existe.
        query.update(page(2, 2, 12));

        assertEquals(2, query.page());
        assertEquals("Pagina 2 de 2 (12 preguntas)", query.summary());
    }

    @Test
    void elResumenUsaSingular() {
        query.update(page(1, 1, 1));

        assertEquals("Pagina 1 de 1 (1 pregunta)", query.summary());
    }
}
