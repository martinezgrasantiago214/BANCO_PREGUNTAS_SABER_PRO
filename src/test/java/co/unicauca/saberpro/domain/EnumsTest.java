package co.unicauca.saberpro.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de los enumerados del dominio (estados, roles, dificultad). */
class EnumsTest {

    @Test
    void hu02ExistenLosEstadosBorradorYPendienteDeRevision() {
        assertEquals("Borrador", QuestionState.BORRADOR.getLabel());
        assertEquals("Pendiente de revision", QuestionState.PENDIENTE_REVISION.getLabel());
    }

    @Test
    void hu02CadaEstadoTieneUnColorDistinto() {
        Set<Object> colores = new HashSet<>();
        for (QuestionState state : QuestionState.values()) {
            assertNotNull(state.getColor());
            assertTrue(colores.add(state.getColor()), "Color repetido en " + state);
        }
    }

    @Test
    void hu01ExistenLosTresNivelesDeDificultad() {
        assertEquals(3, DifficultyLevel.values().length);
        assertEquals("Medio", DifficultyLevel.MEDIO.toString());
    }

    @Test
    void existenLosTresRolesDelSistema() {
        assertEquals("Administrador", Role.ADMINISTRADOR.getLabel());
        assertEquals("Autor de preguntas", Role.AUTOR_PREGUNTAS.getLabel());
        assertEquals("Revisor", Role.REVISOR.getLabel());
    }

    @Test
    void estadosDeUsuario() {
        assertEquals("Activo", UserStatus.ACTIVO.toString());
        assertEquals("Inactivo", UserStatus.INACTIVO.toString());
    }
}
