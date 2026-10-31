package co.unicauca.saberpro.questions.domain.validation;

import co.unicauca.saberpro.questions.domain.model.QuestionData;

import java.util.Optional;

/** Diseno Centrado en Evidencia: justificacion, bibliografia y autor identificado. */
public class EvidenceMetadataRule extends QuestionValidationHandler {

    @Override
    protected Optional<String> check(QuestionData d) {
        if (isBlank(d.justification()) || isBlank(d.bibliography())) {
            return Optional.of("Debe registrar la justificación de la respuesta y la bibliografía.");
        }
        if (isBlank(d.authorLogin())) {
            return Optional.of("La pregunta debe tener un autor identificado.");
        }
        return Optional.empty();
    }
}
