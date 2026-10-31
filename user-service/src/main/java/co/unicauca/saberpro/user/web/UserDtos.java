package co.unicauca.saberpro.user.web;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.user.domain.User;

/** DTOs del API del user-service. El hash de la contrasena nunca sale del servicio. */
public final class UserDtos {

    private UserDtos() {
    }

    public record LoginRequest(String login, String password) {
    }

    public record RegisterUserRequest(String login, String fullName, String email, Role role, String password) {
    }

    public record UserResponse(int userId, String login, String fullName, String email, Role role,
                               String status) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getUserId(), user.getLogin(), user.getFullName(), user.getEmail(),
                    user.getRole(), user.getStatus().name());
        }
    }
}
