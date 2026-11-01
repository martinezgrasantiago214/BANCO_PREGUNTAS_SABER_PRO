package co.unicauca.saberpro.user.service;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.user.domain.User;
import co.unicauca.saberpro.user.domain.UserStatus;
import co.unicauca.saberpro.user.testdoubles.InMemoryUserRepositoryFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(new InMemoryUserRepositoryFake(),
                new DefaultPasswordPolicy(), new PBKDF2PasswordHasher());
    }

    @Test
    void registraUnUsuarioValido() {
        OperationResult result = userService.register("autor1", "Autor Uno", "autor1@x.co",
                Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "Autor123!");
        assertTrue(result.isSuccess());
        assertTrue(userService.findByLogin("autor1").isPresent());
    }

    @Test
    void rechazaDatosObligatoriosFaltantes() {
        assertFalse(userService.register("", "Autor", "a@x.co", Role.AUTOR_PREGUNTAS,
                UserStatus.ACTIVO, "Autor123!").isSuccess());
        assertFalse(userService.register("autor1", " ", "a@x.co", Role.AUTOR_PREGUNTAS,
                UserStatus.ACTIVO, "Autor123!").isSuccess());
        assertFalse(userService.register("autor1", "Autor", "a@x.co", null,
                UserStatus.ACTIVO, "Autor123!").isSuccess());
        assertFalse(userService.register("autor1", "Autor", "a@x.co", Role.AUTOR_PREGUNTAS,
                null, "Autor123!").isSuccess());
    }

    @Test
    void rechazaContrasenaQueNoCumpleLaPolitica() {
        OperationResult result = userService.register("autor1", "Autor Uno", "autor1@x.co",
                Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "debil");
        assertFalse(result.isSuccess());
        assertFalse(result.getErrorsAsText().isEmpty());
    }

    @Test
    void rechazaLoginDuplicado() {
        userService.register("autor1", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "Autor123!");
        OperationResult result = userService.register("autor1", "Otro", "b@x.co", Role.AUTOR_PREGUNTAS,
                UserStatus.ACTIVO, "Otro123!");
        assertFalse(result.isSuccess());
    }

    @Test
    void loginCorrectoDevuelveElUsuario() {
        userService.register("autor1", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "Autor123!");
        Optional<User> result = userService.login("autor1", "Autor123!");
        assertTrue(result.isPresent());
        assertEquals("autor1", result.get().getLogin());
    }

    @Test
    void loginConContrasenaIncorrectaONulaFalla() {
        userService.register("autor1", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "Autor123!");
        assertTrue(userService.login("autor1", "OtraClave1!").isEmpty());
        assertTrue(userService.login("autor1", null).isEmpty());
        assertTrue(userService.login("noexiste", "Autor123!").isEmpty());
    }

    @Test
    void loginDeUsuarioInactivoFalla() {
        userService.register("autor1", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS, UserStatus.INACTIVO, "Autor123!");
        assertTrue(userService.login("autor1", "Autor123!").isEmpty());
    }

    @Test
    void listReviewersSoloDevuelveUsuariosConRolRevisor() {
        userService.register("rev1", "Revisor Uno", "r@x.co", Role.REVISOR, UserStatus.ACTIVO, "Revisor123!");
        userService.register("autor1", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "Autor123!");

        List<User> reviewers = userService.listReviewers();

        assertEquals(1, reviewers.size());
        assertEquals(Role.REVISOR, reviewers.get(0).getRole());
        assertEquals(2, userService.listUsers().size());
    }
}
