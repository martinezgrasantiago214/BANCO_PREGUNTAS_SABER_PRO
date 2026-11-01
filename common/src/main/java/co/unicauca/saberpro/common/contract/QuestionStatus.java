package co.unicauca.saberpro.common.contract;

/**
 * Estados del ciclo de vida de una pregunta tal como viajan en el API REST y
 * en los eventos. Las reglas de transicion entre estados viven en el
 * question-service (patron State); aqui solo esta el contrato.
 *
 * HU02: los estados se visualizan con colores, por eso cada estado lleva su
 * color en hexadecimal (el backend no depende de java.awt).
 */
public enum QuestionStatus {
    BORRADOR("Borrador", "#FFE082"),                        // amarillo
    PENDIENTE_REVISION("Pendiente de revision", "#90CAF9"), // azul
    APROBADA("Aprobada", "#A5D6A7"),                        // verde
    RECHAZADA("Rechazada", "#EF9A9A"),                      // rojo
    ELIMINADA("Eliminada", "#E0E0E0");                      // gris

    private final String label;
    private final String colorHex;

    QuestionStatus(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() {
        return label;
    }

    public String getColorHex() {
        return colorHex;
    }

    @Override
    public String toString() {
        return label;
    }
}
