package co.unicauca.saberpro.users.application;

import co.unicauca.saberpro.users.domain.exception.UserDomainException;
import co.unicauca.saberpro.users.domain.model.Role;
import co.unicauca.saberpro.users.domain.model.User;
import co.unicauca.saberpro.users.domain.policy.DefaultPasswordPolicy;
import co.unicauca.saberpro.users.testdoubles.FakePasswordHasher;
import co.unicauca.saberpro.users.testdoubles.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private InMemoryUserRepository repository;
    private UserService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        service = new UserService(repository, new DefaultPasswordPolicy(), new FakePasswordHasher());
    }

    @Test
    void registersUserWithHashedPassword() {
        User user = service.register("rev1", "Revisor Uno", "rev1@x.co", Role.REVISOR, "Revisor123!");
        assertNotNull(user.getId());
        assertEquals("hashed:Revisor123!", user.getPasswordHash());
        assertTrue(repository.existsByLogin("rev1"));
    }

    @Test
    void rejectsInvalidDataWithAllErrors() {
        UserDomainException ex = assertThrows(UserDomainException.class,
                () -> service.register("", "", "sin-arroba", null, "abc"));
        assertEquals(UserDomainException.Kind.VALIDATION, ex.getKind());
        assertTrue(ex.getDetails().size() >= 5);
    }

    @Test
    void rejectsDuplicatedLogin() {
        service.register("rev1", "Revisor Uno", "rev1@x.co", Role.REVISOR, "Revisor123!");
        UserDomainException ex = assertThrows(UserDomainException.class,
                () -> service.register("rev1", "Otro", "otro@x.co", Role.REVISOR, "Revisor123!"));
        assertEquals(UserDomainException.Kind.DUPLICATED, ex.getKind());
    }

    @Test
    void logsInWithValidCredentials() {
        service.register("admin", "Admin", "admin@x.co", Role.ADMINISTRADOR, "Admin123!");
        assertEquals(Role.ADMINISTRADOR, service.login("admin", "Admin123!").getRole());
    }

    @Test
    void rejectsWrongPasswordUnknownUserAndInactiveAccount() {
        User user = service.register("autor1", "Autor", "autor1@x.co", Role.AUTOR_PREGUNTAS, "Autor123!");
        assertEquals(UserDomainException.Kind.INVALID_CREDENTIALS,
                assertThrows(UserDomainException.class, () -> service.login("autor1", "Mala123!")).getKind());
        assertThrows(UserDomainException.class, () -> service.login("nadie", "Autor123!"));
        user.deactivate();
        assertThrows(UserDomainException.class, () -> service.login("autor1", "Autor123!"));
    }

    @Test
    void listsByRoleAndFindsByLogin() {
        service.register("rev1", "R1", "r1@x.co", Role.REVISOR, "Revisor123!");
        service.register("rev2", "R2", "r2@x.co", Role.REVISOR, "Revisor123!");
        service.register("autor1", "A1", "a1@x.co", Role.AUTOR_PREGUNTAS, "Autor123!");
        assertEquals(2, service.list(Role.REVISOR).size());
        assertEquals(3, service.list(null).size());
        assertEquals("R1", service.findByLogin("rev1").getFullName());
        assertEquals(UserDomainException.Kind.NOT_FOUND,
                assertThrows(UserDomainException.class, () -> service.findByLogin("x")).getKind());
    }

    @Test
    void registerIfMissingIsIdempotent() {
        service.registerIfMissing("admin", "Admin", "admin@x.co", Role.ADMINISTRADOR, "Admin123!");
        service.registerIfMissing("admin", "Admin", "admin@x.co", Role.ADMINISTRADOR, "Admin123!");
        assertEquals(1, service.list(null).size());
    }
}
