package co.unicauca.saberpro.api.dto;

import co.unicauca.saberpro.domain.User;

/** Representacion JSON de un usuario. NUNCA expone el hash de la contrasena. */
public record UserResponseDTO(String login, String fullName, String email, String role, String status) {

    public static UserResponseDTO from(User u) {
        return new UserResponseDTO(
                u.getLogin(),
                u.getFullName(),
                u.getEmail(),
                u.getRole() != null ? u.getRole().name() : null,
                u.getStatus() != null ? u.getStatus().name() : null);
    }
}
