package com.stgian.dto;

import com.stgian.model.StockMovement;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class StockDTOs {

    // Entrada manual (reposição)
    public record StockEntryRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        @NotBlank String reason
    ) {}

    // Saída manual (perda, descarte, ajuste)
    public record StockExitRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        @NotBlank String reason
    ) {}

    // Resposta de movimentação
    public record MovementResponse(
        Long id,
        Long productId,
        String productName,
        String productCategory,
        String type,
        String typeLabel,
        Integer quantity,
        Integer stockBefore,
        Integer stockAfter,
        String reason,
        String registeredBy,
        String orderCode,
        LocalDateTime createdAt
    ) {
        public static MovementResponse from(StockMovement m) {
            String label = switch (m.getType()) {
                case ENTRADA -> "Entrada";
                case SAIDA   -> "Saída Manual";
                case VENDA   -> "Saída — Venda";
            };
            return new MovementResponse(
                m.getId(),
                m.getProduct().getId(),
                m.getProduct().getName(),
                m.getProduct().getCategory().name(),
                m.getType().name(),
                label,
                m.getQuantity(),
                m.getStockBefore(),
                m.getStockAfter(),
                m.getReason(),
                m.getRegisteredBy() != null ? m.getRegisteredBy().getName() : "Sistema",
                m.getOrderCode(),
                m.getCreatedAt()
            );
        }
    }

    // Resumo por produto
    public record StockSummary(
        Long productId,
        String productName,
        String category,
        Integer currentStock,
        Integer totalEntradas,
        Integer totalSaidas,
        boolean lowStock
    ) {}
}
