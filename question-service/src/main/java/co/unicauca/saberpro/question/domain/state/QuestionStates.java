package co.unicauca.saberpro.question.domain.state;

import co.unicauca.saberpro.common.contract.QuestionStatus;

/** Obtiene el objeto estado que corresponde a un {@link QuestionStatus} (p. ej. al leer de la base de datos). */
public final class QuestionStates {

    private QuestionStates() {
    }

    public static QuestionState from(QuestionStatus status) {
        return switch (status) {
            case BORRADOR -> new BorradorState();
            case PENDIENTE_REVISION -> new PendienteRevisionState();
            case APROBADA -> new AprobadaState();
            case RECHAZADA -> new RechazadaState();
            case ELIMINADA -> new EliminadaState();
        };
    }
}
