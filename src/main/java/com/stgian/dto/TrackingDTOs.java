package com.stgian.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class TrackingDTOs {

    // ADM define o código de rastreio
    public record SetTrackingRequest(
        @NotBlank String trackingCode,
        String carrier   // ex: "Correios", "Jadlog" — opcional
    ) {}

    // Retorno do rastreio
    public record TrackingResponse(
        String orderCode,
        String trackingCode,
        String carrier,
        String trackingUrl,
        String trackingStatus,
        LocalDateTime trackingUpdatedAt,
        // Eventos dos Correios
        java.util.List<TrackingEvent> events
    ) {}

    public record TrackingEvent(
        String date,
        String time,
        String description,
        String location
    ) {}
}
