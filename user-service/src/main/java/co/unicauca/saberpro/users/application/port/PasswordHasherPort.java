package co.unicauca.saberpro.users.application.port;

/** Puerto de salida para el cifrado de contrasenas. */
public interface PasswordHasherPort {

    String hash(String plainPassword);

    boolean verify(String plainPassword, String storedHash);
}
