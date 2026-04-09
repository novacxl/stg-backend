package com.stgian.dto;

import com.stgian.model.Product;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class ProductDTOs {

    public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @Min(1) Integer price,
        @NotNull String category,
        String badge,
        @Min(0) Integer stock,
        String icon,
        // Imagem principal
        @Size(max = 2_000_000, message = "Imagem muito grande (max ~1.5MB)") String imageData,
        // Galeria de imagens adicionais (máx 5 fotos extras)
        @Size(max = 5, message = "Máximo de 5 imagens adicionais") List<String> images
    ) {}

    public record StockUpdateRequest(@NotNull @Min(0) Integer stock) {}

    // Versão leve para listagem — SEM imageData e SEM images (reduz payload em ~95%)
    public record ProductSummary(
        Long id, String name, String description, Integer price,
        String category, String badge, Integer stock, String icon, Boolean active
    ) {
        public static ProductSummary from(Product p) {
            return new ProductSummary(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getCategory() != null ? p.getCategory().name() : null,
                p.getBadge()    != null ? p.getBadge().toString()  : null,
                p.getStock(), p.getIcon(), p.getActive()
            );
        }
    }

    // Versão completa COM imageData e galeria — usada na página do produto e admin
    public record ProductResponse(
        Long id, String name, String description, Integer price,
        String category, String badge, Integer stock, String icon,
        String imageData, List<String> images, Boolean active
    ) {
        public static ProductResponse from(Product p) {
            return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getCategory() != null ? p.getCategory().name() : null,
                p.getBadge()    != null ? p.getBadge().toString()  : null,
                p.getStock(), p.getIcon(), p.getImageData(),
                p.getImages() != null ? p.getImages() : new ArrayList<>(),
                p.getActive()
            );
        }
    }
}
