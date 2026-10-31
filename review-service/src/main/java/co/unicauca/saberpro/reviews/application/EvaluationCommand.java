package co.unicauca.saberpro.reviews.application;

import co.unicauca.saberpro.reviews.domain.model.CriterionScore;
import co.unicauca.saberpro.reviews.domain.model.Verdict;

import java.util.List;

/** Evaluacion que registra un revisor (HU05). */
public record EvaluationCommand(Verdict verdict, List<CriterionScore> scores, String observation) {

    public EvaluationCommand {
        scores = scores == null ? List.of() : List.copyOf(scores);
    }
}
