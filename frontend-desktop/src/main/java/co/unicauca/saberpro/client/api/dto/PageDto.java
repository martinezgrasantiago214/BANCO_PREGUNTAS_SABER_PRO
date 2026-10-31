package co.unicauca.saberpro.client.api.dto;

import java.util.List;

public record PageDto<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
}
