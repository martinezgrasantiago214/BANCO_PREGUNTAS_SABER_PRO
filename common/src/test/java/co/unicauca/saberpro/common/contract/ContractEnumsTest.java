package co.unicauca.saberpro.common.contract;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de los enumerados que viajan en el API (estados, roles, dificultad, veredicto). */
class ContractEnumsTest {

    @Test
    void hu02ExistenLosEstadosBorradorYPendienteDeRevision() {
        assertEquals("Borrador", QuestionStatus.BORRADOR.getLabel());
        assertEquals("Pendiente de revision", QuestionStatus.PENDIENTE_REVISION.toString());
    }

    @Test
    void hu02CadaEstadoTieneUnColorHexadecimalDistinto() {
        Set<String> colores = new HashSet<>();
        for (QuestionStatus status : QuestionStatus.values()) {
            assertTrue(status.getColorHex().matches("#[0-9A-F]{6}"), "Color invalido en " + status);
            assertTrue(colores.add(status.getColorHex()), "Color repetido en " + status);
        }
    }

    @Test
    void hu05ElVeredictoSoloPuedeSerAprobadaORechazada() {
        assertEquals(2, EvaluationVerdict.values().length);
        assertEquals("Aprobada", EvaluationVerdict.APROBADA.toString());
        assertEquals("Rechazada", EvaluationVerdict.RECHAZADA.getLabel());
    }

    @Test
    void hu01ExistenLosTresNivelesDeDificultad() {
        assertEquals(3, DifficultyLevel.values().length);
        assertEquals("Medio", DifficultyLevel.MEDIO.toString());
        assertEquals("Alto", DifficultyLevel.ALTO.getLabel());
    }

    @Test
    void existenLosTresRolesDelSistema() {
        assertEquals("Administrador", Role.ADMINISTRADOR.getLabel());
        assertEquals("Autor de preguntas", Role.AUTOR_PREGUNTAS.toString());
        assertEquals("Revisor", Role.REVISOR.getLabel());
    }
}
