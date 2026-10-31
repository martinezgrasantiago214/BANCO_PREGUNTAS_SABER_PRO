package co.unicauca.saberpro.reviews.infrastructure.web.dto;

import co.unicauca.saberpro.reviews.application.PageResult;

import java.util.List;

public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <T> PageResponse<T> from(PageResult<T> r) {
        return new PageResponse<>(r.items(), r.page(), r.size(), r.totalItems(), r.totalPages());
    }
}
