package co.unicauca.saberpro.client.api.dto;

public record UserDto(Long id, String login, String fullName, String email, String role, String roleLabel,
                      String status) {

    public boolean isAdmin() { return "ADMINISTRADOR".equals(role); }
    public boolean isAuthor() { return "AUTOR_PREGUNTAS".equals(role); }
    public boolean isReviewer() { return "REVISOR".equals(role); }

    @Override
    public String toString() {
        return fullName + " (" + login + ")";
    }
}
