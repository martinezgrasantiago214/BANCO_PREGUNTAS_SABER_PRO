package co.unicauca.saberpro.user.domain;

import co.unicauca.saberpro.common.contract.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de la entidad User y de su estado. */
class UserTest {

    private User nuevoUsuario(UserStatus status) {
        return new User("autor1", "Autor Uno", "autor1@unicauca.edu.co",
                Role.AUTOR_PREGUNTAS, status, "hash-cifrado");
    }

    @Test
    void elConstructorAsignaTodosLosDatos() {
        User u = nuevoUsuario(UserStatus.ACTIVO);

        assertEquals("autor1", u.getLogin());
        assertEquals("Autor Uno", u.getFullName());
        assertEquals("autor1@unicauca.edu.co", u.getEmail());
        assertEquals(Role.AUTOR_PREGUNTAS, u.getRole());
        assertEquals(UserStatus.ACTIVO, u.getStatus());
        assertEquals("hash-cifrado", u.getPasswordHash());
    }

    @Test
    void unUsuarioActivoEstaHabilitado() {
        assertTrue(nuevoUsuario(UserStatus.ACTIVO).isActive());
    }

    @Test
    void unUsuarioInactivoNoEstaHabilitado() {
        assertFalse(nuevoUsuario(UserStatus.INACTIVO).isActive());
    }

    @Test
    void losSettersActualizanLosDatos() {
        User u = new User();
        u.setUserId(7);
        u.setLogin("rev1");
        u.setFullName("Revisor Uno");
        u.setEmail("rev1@x.co");
        u.setRole(Role.REVISOR);
        u.setStatus(UserStatus.ACTIVO);
        u.setPasswordHash("h");

        assertEquals(7, u.getUserId());
        assertEquals("rev1", u.getLogin());
        assertEquals("Revisor Uno", u.getFullName());
        assertEquals("rev1@x.co", u.getEmail());
        assertEquals(Role.REVISOR, u.getRole());
        assertEquals("h", u.getPasswordHash());
        assertTrue(u.isActive());
    }

    @Test
    void toStringMuestraNombreLoginYRol() {
        String texto = nuevoUsuario(UserStatus.ACTIVO).toString();
        assertTrue(texto.contains("Autor Uno"));
        assertTrue(texto.contains("autor1"));
        assertTrue(texto.contains(Role.AUTOR_PREGUNTAS.getLabel()));
    }

    @Test
    void losEstadosDeUsuarioTienenEtiqueta() {
        assertEquals("Activo", UserStatus.ACTIVO.toString());
        assertEquals("Inactivo", UserStatus.INACTIVO.getLabel());
    }
}
