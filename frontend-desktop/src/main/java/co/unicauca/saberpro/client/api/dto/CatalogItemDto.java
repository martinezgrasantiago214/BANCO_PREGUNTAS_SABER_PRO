package co.unicauca.saberpro.client.api.dto;

public record CatalogItemDto(String code, String label, String color) {

    @Override
    public String toString() {
        return label;
    }
}
