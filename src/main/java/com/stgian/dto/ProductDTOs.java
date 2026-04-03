package com.stgian.dto;

import com.stgian.model.Product;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProductDTOs {

    public record ProductRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @NotNull @Min(1) Integer price,
        @NotNull String category,
        String badge,
        @Min(0) Integer stock,
        String icon,
        @Size(max = 2_000_000, message = "Imagem muito grande (max ~1.5MB)") String imageData
    ) {}

    public record StockUpdateRequest(@NotNull @Min(0) Integer stock) {}

    public record ProductResponse(
        Long id,
        String name,
        @Size(max = 500) String description,
        int price,
        String category,
        String badge,
        int stock,
        String icon,
        String imageData,
        boolean active
    ) {
        public static ProductResponse from(Product p) {
            return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getCategory().name(),
                p.getBadge() != null ? p.getBadge().toString() : null,
                p.getStock(),
                p.getIcon(),
                p.getImageData(),
                p.getActive()
            );
        }
    }
}
