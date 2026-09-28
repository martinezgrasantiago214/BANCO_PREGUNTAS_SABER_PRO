package co.unicauca.saberpro.service;

import java.util.List;

/**
 * Abstraccion para la politica de validacion de contrasenas (OCP): si en el
 * futuro cambian las reglas basta con crear una nueva implementacion.
 */
public interface IPasswordPolicy {

    List<String> validate(String plainPassword);
}
