package co.unicauca.saberpro.api.dto;

import java.util.List;

/** Cuerpo JSON para que el administrador asigne revisores (HU04). */
public class AssignReviewersDTO {

    private List<String> reviewers;

    public AssignReviewersDTO() {
    }

    public List<String> getReviewers() { return reviewers; }
    public void setReviewers(List<String> reviewers) { this.reviewers = reviewers; }
}
