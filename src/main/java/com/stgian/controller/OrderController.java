package com.stgian.controller;

import com.stgian.dto.OrderDTOs;
import com.stgian.security.JwtUtil;
import com.stgian.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    public OrderController(OrderService orderService, JwtUtil jwtUtil) {
        this.orderService = orderService;
        this.jwtUtil      = jwtUtil;
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN','OWNER')")
    public ResponseEntity<OrderDTOs.OrderResponse> checkout(
            @RequestHeader("Authorization") String bearer,
            @Valid @RequestBody OrderDTOs.CheckoutRequest req) {
        Long userId = extractUserId(bearer);
        OrderDTOs.OrderResponse order = orderService.checkout(userId, req);
        return ResponseEntity.status(201).body(order);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN','OWNER')")
    public ResponseEntity<List<OrderDTOs.OrderResponse>> myOrders(
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(orderService.getMyOrders(extractUserId(bearer)));
    }

    @GetMapping("/track/{code}")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN','OWNER')")
    public ResponseEntity<OrderDTOs.OrderResponse> track(
            @RequestHeader("Authorization") String bearer,
            @PathVariable String code) {
        return ResponseEntity.ok(orderService.trackByCode(code, extractUserId(bearer)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<OrderDTOs.OrderResponse>> allOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /** GET /api/orders/{id} — detalhe completo de um pedido (admin/owner) */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<OrderDTOs.OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<OrderDTOs.OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderDTOs.StatusUpdateRequest req) {
        return ResponseEntity.ok(orderService.updateStatus(id, req));
    }

    private Long extractUserId(String bearer) {
        return jwtUtil.extractUserId(bearer.substring(7));
    }
}
