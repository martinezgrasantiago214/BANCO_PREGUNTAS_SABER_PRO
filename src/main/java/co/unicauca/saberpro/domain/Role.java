package co.unicauca.saberpro.domain;

/**
 * Roles disponibles para un usuario del sistema.
 * Reutilizado y adaptado del Taller SOLID de Gestion de Usuarios.
 */
public enum Role {
    ADMINISTRADOR("Administrador"),
    AUTOR_PREGUNTAS("Autor de preguntas"),
    REVISOR("Revisor");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
