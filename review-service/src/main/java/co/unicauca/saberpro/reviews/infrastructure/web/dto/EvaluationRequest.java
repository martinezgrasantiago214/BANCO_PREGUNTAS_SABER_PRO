package co.unicauca.saberpro.reviews.infrastructure.web.dto;

import co.unicauca.saberpro.reviews.application.EvaluationCommand;
import co.unicauca.saberpro.reviews.domain.model.CriterionScore;
import co.unicauca.saberpro.reviews.domain.model.ReviewCriterion;
import co.unicauca.saberpro.reviews.domain.model.Verdict;

import java.util.List;

public record EvaluationRequest(Verdict verdict, List<Score> scores, String observation) {

    public record Score(ReviewCriterion criterion, int score, String comment) {
    }

    public EvaluationCommand toCommand() {
        List<CriterionScore> mapped = scores == null ? List.of()
                : scores.stream().map(s -> new CriterionScore(s.criterion(), s.score(), s.comment())).toList();
        return new EvaluationCommand(verdict, mapped, observation);
    }
}
