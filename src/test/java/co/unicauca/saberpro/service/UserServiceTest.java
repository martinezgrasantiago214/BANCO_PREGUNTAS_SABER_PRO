package co.unicauca.saberpro.service;

import co.unicauca.saberpro.domain.Role;
import co.unicauca.saberpro.domain.User;
import co.unicauca.saberpro.domain.UserStatus;
import co.unicauca.saberpro.testdoubles.InMemoryUserRepositoryFake;
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
    }

    @Test
    void rechazaContrasenaQueNoCumpleLaPolitica() {
        OperationResult result = userService.register("autor1", "Autor Uno", "autor1@x.co",
                Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "debil");
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void rechazaLoginDuplicado() {
        userService.register("autor1", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "Autor123!");
        OperationResult result = userService.register("autor1", "Otro", "b@x.co", Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "Otro123!");
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
    void loginConContrasenaIncorrectaFalla() {
        userService.register("autor1", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "Autor123!");
        assertTrue(userService.login("autor1", "OtraClave1!").isEmpty());
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
    }
}
