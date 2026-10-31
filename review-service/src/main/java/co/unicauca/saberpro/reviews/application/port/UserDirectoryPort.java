package co.unicauca.saberpro.reviews.application.port;

/** Consulta al contexto de usuarios (adaptador REST con balanceo por Eureka). */
public interface UserDirectoryPort {

    /** @return true si el usuario existe, esta activo y tiene el rol indicado. */
    boolean hasActiveRole(String login, String role);
}
