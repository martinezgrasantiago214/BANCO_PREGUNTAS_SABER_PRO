package co.unicauca.saberpro.reviews.domain.model;

/** Estado del proceso de revision de una pregunta. */
public enum ReviewStatus {
    PENDIENTE_ASIGNACION("Pendiente de asignación"),
    EN_REVISION("En revisión"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada");

    private final String label;

    ReviewStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
