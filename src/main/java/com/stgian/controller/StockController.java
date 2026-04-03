package com.stgian.controller;

import com.stgian.dto.StockDTOs;
import com.stgian.security.JwtUtil;
import com.stgian.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
public class StockController {

    private final StockService stockService;
    private final JwtUtil jwtUtil;

    public StockController(StockService stockService, JwtUtil jwtUtil) {
        this.stockService = stockService;
        this.jwtUtil = jwtUtil;
    }

    // Histórico completo de movimentações
    @GetMapping("/movements")
    public ResponseEntity<List<StockDTOs.MovementResponse>> listAll() {
        return ResponseEntity.ok(stockService.listAll());
    }

    // Histórico de um produto específico
    @GetMapping("/movements/product/{productId}")
    public ResponseEntity<List<StockDTOs.MovementResponse>> listByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockService.listByProduct(productId));
    }

    // Últimas N movimentações
    @GetMapping("/movements/recent")
    public ResponseEntity<List<StockDTOs.MovementResponse>> listRecent(
            @RequestParam(defaultValue = "20") int limit) {
        // Teto de 200 para evitar DoS por requisição excessiva
        int safeLimit = Math.min(Math.max(1, limit), 200);
        return ResponseEntity.ok(stockService.listRecent(safeLimit));
    }

    // Resumo de estoque de todos os produtos
    @GetMapping("/summary")
    public ResponseEntity<List<StockDTOs.StockSummary>> summary() {
        return ResponseEntity.ok(stockService.getSummary());
    }

    // Registrar entrada (reposição)
    @PostMapping("/entry")
    public ResponseEntity<StockDTOs.MovementResponse> registerEntry(
            @RequestHeader("Authorization") String bearer,
            @Valid @RequestBody StockDTOs.StockEntryRequest req) {
        Long userId = jwtUtil.extractUserId(bearer.substring(7));
        return ResponseEntity.status(201).body(stockService.registerEntry(userId, req));
    }

    // Registrar saída manual (perda, descarte)
    @PostMapping("/exit")
    public ResponseEntity<StockDTOs.MovementResponse> registerExit(
            @RequestHeader("Authorization") String bearer,
            @Valid @RequestBody StockDTOs.StockExitRequest req) {
        Long userId = jwtUtil.extractUserId(bearer.substring(7));
        return ResponseEntity.status(201).body(stockService.registerExit(userId, req));
    }
}
