package co.unicauca.saberpro.users.testdoubles;

import co.unicauca.saberpro.users.application.port.PasswordHasherPort;

/** Hasher trivial y determinista para pruebas. */
public class FakePasswordHasher implements PasswordHasherPort {

    @Override
    public String hash(String plainPassword) {
        return "hashed:" + plainPassword;
    }

    @Override
    public boolean verify(String plainPassword, String storedHash) {
        return ("hashed:" + plainPassword).equals(storedHash);
    }
}
