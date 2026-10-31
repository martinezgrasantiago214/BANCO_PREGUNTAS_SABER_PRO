package co.unicauca.saberpro.notifications.application;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> items, int page, int size, long totalItems) {

    public int totalPages() {
        return (int) Math.max(1, Math.ceil((double) totalItems / Math.max(1, size)));
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalItems);
    }
}
