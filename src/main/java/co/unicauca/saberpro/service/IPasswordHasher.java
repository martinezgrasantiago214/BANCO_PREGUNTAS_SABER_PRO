package co.unicauca.saberpro.service;

/** Abstraccion para el cifrado y verificacion de contrasenas (ISP). */
public interface IPasswordHasher {

    String hash(String plainPassword);

    boolean verify(String plainPassword, String storedHash);
}
