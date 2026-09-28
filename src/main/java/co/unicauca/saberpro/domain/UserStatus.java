package co.unicauca.saberpro.domain;

/** Estado de un usuario dentro del sistema. */
public enum UserStatus {
    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    private final String label;

    UserStatus(String label) {
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
