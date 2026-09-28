package co.unicauca.saberpro.service;

import co.unicauca.saberpro.domain.QuestionState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de OperationResult, PagedResult y QuestionFilterCriteria (HU03). */
class SupportClassesTest {

    @Test
    void operationResultOkSinErrores() {
        OperationResult r = OperationResult.ok("dato");
        assertTrue(r.isSuccess());
        assertTrue(r.getErrors().isEmpty());
        assertEquals("dato", r.getData());
    }

    @Test
    void operationResultFailConErrores() {
        OperationResult r = OperationResult.fail(List.of("error 1", "error 2"));
        assertFalse(r.isSuccess());
        assertEquals("error 1\nerror 2", r.getErrorsAsText());
        assertNull(r.getData());
    }

    @Test
    void hu03PagedResultCalculaElTotalDePaginas() {
        assertEquals(3, new PagedResult<>(List.of("a"), 1, 5, 11).getTotalPages());
        assertEquals(2, new PagedResult<>(List.of("a"), 1, 5, 10).getTotalPages());
        assertEquals(1, new PagedResult<>(List.of(), 1, 5, 0).getTotalPages());
    }

    @Test
    void hu03CriteriosDeFiltroNoAceptanPaginasInvalidas() {
        QuestionFilterCriteria c = new QuestionFilterCriteria()
                .setAuthorLogin("autor1").setState(QuestionState.BORRADOR)
                .setTopic("DDD").setKeyword("patron").setPage(0).setPageSize(-3);

        assertEquals("autor1", c.getAuthorLogin());
        assertEquals(QuestionState.BORRADOR, c.getState());
        assertEquals("DDD", c.getTopic());
        assertEquals("patron", c.getKeyword());
        assertEquals(1, c.getPage());
        assertEquals(1, c.getPageSize());
    }
}
