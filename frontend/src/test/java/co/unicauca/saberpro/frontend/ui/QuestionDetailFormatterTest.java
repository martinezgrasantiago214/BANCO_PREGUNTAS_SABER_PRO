package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.frontend.model.Models.QuestionView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** HU03: el detalle muestra todos los datos de la pregunta y marca la respuesta correcta. */
class QuestionDetailFormatterTest {

    @Test
    void muestraTodosLosCamposYMarcaLaRespuestaCorrecta() {
        QuestionView q = new QuestionView("q-1", "MULTIPLE_CHOICE", "Un contexto", "¿Una pregunta?",
                List.of("Capas", "Microkernel", "Broker", "Eventos"), "Microkernel", "Una justificacion",
                "Una bibliografia", "Arquitectura", "Patrones", "Microkernel", DifficultyLevel.MEDIO,
                QuestionStatus.BORRADOR, "Borrador", "#FFE082", true, "autor1", "2026-10-31T10:00:00");

        String text = QuestionDetailFormatter.format(q);

        assertTrue(text.contains("Estado: Borrador"));
        assertTrue(text.contains("Un contexto"));
        assertTrue(text.contains("¿Una pregunta?"));
        assertTrue(text.contains("A) Capas\n"));
        assertTrue(text.contains("B) Microkernel   <-- respuesta correcta"));
        assertTrue(text.contains("Una justificacion"));
        assertTrue(text.contains("Una bibliografia"));
        assertTrue(text.contains("Subtema: Microkernel"));
        assertTrue(text.contains("Nivel de dificultad: Medio"));
    }
}
