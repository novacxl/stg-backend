package com.stgian.dto;

import com.stgian.model.Product;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductDTOs {

    public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @Min(1) Integer price,
        @NotNull String category,
        String badge,
        @Min(0) Integer stock,
        String icon,
        @Size(max = 2_000_000, message = "Imagem muito grande (max ~1.5MB)") String imageData
    ) {}

    public record StockUpdateRequest(@NotNull @Min(0) Integer stock) {}

    /**
     * PERF-2: ProductSummary — versão leve SEM imageData para listagens.
     * Reduz o payload da lista de ~200KB para ~2KB quando há imagens.
     * O frontend busca a imagem completa só quando precisar (página do produto).
     */
    public record ProductSummary(
        Long id,
        String name,
        String description,
        Integer price,
        String category,
        String badge,
        Integer stock,
        String icon,
        Boolean active
    ) {
        public static ProductSummary from(Product p) {
            return new ProductSummary(
                p.getId(), p.getName(), p.getDescription(),
                p.getPrice(),
                p.getCategory() != null ? p.getCategory().name() : null,
                p.getBadge() != null ? p.getBadge().toString() : null,
                p.getStock(), p.getIcon(), p.getActive()
            );
        }
    }

    /**
     * ProductResponse — versão completa COM imageData.
     * Usada apenas para: GET /products/{id} (página individual do produto)
     * e operações de admin (edição).
     */
    public record ProductResponse(
        Long id,
        String name,
        String description,
        Integer price,
        String category,
        String badge,
        Integer stock,
        String icon,
        String imageData,
        Boolean active
    ) {
        public static ProductResponse from(Product p) {
            return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getPrice(),
                p.getCategory() != null ? p.getCategory().name() : null,
                p.getBadge() != null ? p.getBadge().toString() : null,
                p.getStock(), p.getIcon(), p.getImageData(), p.getActive()
            );
        }
    }
}
