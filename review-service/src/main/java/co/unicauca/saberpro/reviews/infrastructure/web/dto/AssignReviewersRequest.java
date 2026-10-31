package co.unicauca.saberpro.reviews.infrastructure.web.dto;

import java.util.List;

public record AssignReviewersRequest(List<String> reviewerLogins) {
}
