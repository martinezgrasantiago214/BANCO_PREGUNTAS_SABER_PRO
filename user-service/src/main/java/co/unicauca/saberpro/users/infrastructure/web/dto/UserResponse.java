package co.unicauca.saberpro.users.infrastructure.web.dto;

import co.unicauca.saberpro.users.domain.model.User;

/** Representacion publica del usuario: nunca expone el hash de la contrasena. */
public record UserResponse(Long id, String login, String fullName, String email,
                           String role, String roleLabel, String status) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getLogin(), u.getFullName(), u.getEmail(),
                u.getRole().name(), u.getRole().getLabel(), u.getStatus().name());
    }
}
