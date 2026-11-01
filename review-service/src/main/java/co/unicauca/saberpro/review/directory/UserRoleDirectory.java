package co.unicauca.saberpro.review.directory;

import co.unicauca.saberpro.common.contract.Role;

import java.util.Optional;

/**
 * Puerto para consultar el rol de un usuario (lo tiene el user-service). El
 * review-service lo usa en HU04 (PSP-31) para aceptar solo revisores.
 */
public interface UserRoleDirectory {

    /**
     * @return el rol del usuario, o vacio si no existe.
     * @throws co.unicauca.saberpro.common.web.BusinessException si no fue posible consultar el user-service.
     */
    Optional<Role> findRole(String login);
}
