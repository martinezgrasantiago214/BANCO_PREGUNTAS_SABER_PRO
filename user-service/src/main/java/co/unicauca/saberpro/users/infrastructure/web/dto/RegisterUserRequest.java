package co.unicauca.saberpro.users.infrastructure.web.dto;

import co.unicauca.saberpro.users.domain.model.Role;

public record RegisterUserRequest(String login, String fullName, String email, Role role, String password) {
}
