package co.unicauca.saberpro.domain;

import java.awt.Color;

/**
 * Estados del ciclo de vida de una pregunta del banco Saber PRO.
 *
 * HU02: los estados se deben visualizar con colores, por eso cada estado
 * conoce el color con el que debe representarse en la interfaz grafica.
 */
public enum QuestionState {
    BORRADOR("Borrador", new Color(255, 224, 130)),                 // amarillo
    PENDIENTE_REVISION("Pendiente de revision", new Color(144, 202, 249)), // azul
    EN_REVISION("En revision", new Color(179, 157, 219)),            // morado
    APROBADA("Aprobada", new Color(165, 214, 167)),                  // verde
    RECHAZADA("Rechazada", new Color(239, 154, 154)),                // rojo
    ELIMINADA("Eliminada", new Color(224, 224, 224));                // gris

    private final String label;
    private final Color color;

    QuestionState(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return label;
    }
}
