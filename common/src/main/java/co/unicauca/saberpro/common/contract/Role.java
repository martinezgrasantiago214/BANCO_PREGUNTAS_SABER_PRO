package co.unicauca.saberpro.common.contract;

/** Roles de un usuario del sistema; viajan en el API y el frontend los usa para armar el menu. */
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
