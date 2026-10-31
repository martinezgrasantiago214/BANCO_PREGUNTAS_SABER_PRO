package co.unicauca.saberpro.questions.domain.validation;

/** Arma la cadena estandar de validacion estructural. */
public final class QuestionValidationChain {

    private QuestionValidationChain() {
    }

    public static QuestionValidationHandler standard() {
        QuestionValidationHandler head = new ContentRule();
        head.linkWith(new OptionsRule())
            .linkWith(new CorrectAnswerRule())
            .linkWith(new ClassificationRule())
            .linkWith(new EvidenceMetadataRule());
        return head;
    }
}
