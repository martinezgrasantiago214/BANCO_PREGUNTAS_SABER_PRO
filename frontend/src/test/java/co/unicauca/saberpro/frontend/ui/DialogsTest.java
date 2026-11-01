package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.frontend.client.ApiException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** PSP-17: el formulario muestra como lista los errores de validacion que devuelve el servidor. */
class DialogsTest {

    @Test
    void variosErroresSeMuestranComoLista() {
        ApiException ex = new ApiException(400, List.of("El distractor C esta vacio.",
                "Debe indicar Competencia, Tema, Subtema y Nivel de dificultad."));

        assertEquals("Corrija lo siguiente:\n\n  • El distractor C esta vacio.\n"
                + "  • Debe indicar Competencia, Tema, Subtema y Nivel de dificultad.", Dialogs.formatMessages(ex));
    }

    @Test
    void unSoloErrorSeMuestraTalCual() {
        ApiException ex = new ApiException(400, List.of("Tipo de pregunta no soportado."));

        assertEquals("Tipo de pregunta no soportado.", Dialogs.formatMessages(ex));
    }
}
