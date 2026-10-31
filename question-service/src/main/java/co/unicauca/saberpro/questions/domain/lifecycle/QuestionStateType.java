package co.unicauca.saberpro.questions.domain.lifecycle;

/**
 * Estados del ciclo de vida (contexto "Ciclo de Vida de Preguntas").
 * HU02: cada estado tiene un color para visualizarlo en la interfaz.
 */
public enum QuestionStateType {
    BORRADOR("Borrador", "#FFE082"),
    PENDIENTE_REVISION("Pendiente de revisión", "#90CAF9"),
    EN_REVISION("En revisión", "#B39DDB"),
    APROBADA("Aprobada", "#A5D6A7"),
    RECHAZADA("Rechazada", "#EF9A9A");

    private final String label;
    private final String colorHex;

    QuestionStateType(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() { return label; }
    public String getColorHex() { return colorHex; }

    /** Devuelve el objeto State que implementa el comportamiento de este estado. */
    public QuestionState toState() {
        return switch (this) {
            case BORRADOR -> DraftState.INSTANCE;
            case PENDIENTE_REVISION -> PendingReviewState.INSTANCE;
            case EN_REVISION -> InReviewState.INSTANCE;
            case APROBADA -> ApprovedState.INSTANCE;
            case RECHAZADA -> RejectedState.INSTANCE;
        };
    }
}
