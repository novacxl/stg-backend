package com.stgian.controller;

import com.stgian.dto.TrackingDTOs;
import com.stgian.security.JwtUtil;
import com.stgian.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final TrackingService trackingService;
    private final JwtUtil jwtUtil;

    public TrackingController(TrackingService trackingService, JwtUtil jwtUtil) {
        this.trackingService = trackingService;
        this.jwtUtil = jwtUtil;
    }

    // ADM/OWNER define o código de rastreio de um pedido
    @PostMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<TrackingDTOs.TrackingResponse> setTracking(
            @PathVariable Long orderId,
            @Valid @RequestBody TrackingDTOs.SetTrackingRequest req) {
        return ResponseEntity.ok(trackingService.setTracking(orderId, req));
    }

    // Cliente/ADM consulta rastreio pelo código do pedido
    @GetMapping("/order/{orderCode}")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN','OWNER')")
    public ResponseEntity<TrackingDTOs.TrackingResponse> getTracking(
            @PathVariable String orderCode,
            @RequestHeader("Authorization") String bearer) {
        Long userId = jwtUtil.extractUserId(bearer.substring(7));
        return ResponseEntity.ok(trackingService.getTracking(orderCode, userId));
    }
}
