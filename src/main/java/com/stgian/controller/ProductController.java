package com.stgian.controller;

import com.stgian.dto.ProductDTOs;
import com.stgian.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * PERF-1 + PERF-2: Lista retorna ProductSummary (sem imageData) com cache de 2 min.
     * Antes: cada produto com imagem = ~150KB. Agora: lista inteira = ~3KB.
     */
    @GetMapping
    public ResponseEntity<List<ProductDTOs.ProductSummary>> list(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(2, TimeUnit.MINUTES).mustRevalidate())
            .body(productService.findAll(category));
    }

    /**
     * Produto completo com imageData — chamado ao abrir a página do produto.
     * Cache de 5 minutos.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTOs.ProductResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).mustRevalidate())
            .body(productService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ProductDTOs.ProductResponse> create(
            @Valid @RequestBody ProductDTOs.ProductRequest req) {
        return ResponseEntity.status(201).body(productService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ProductDTOs.ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductDTOs.ProductRequest req) {
        return ResponseEntity.ok(productService.update(id, req));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ProductDTOs.ProductResponse> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody ProductDTOs.StockUpdateRequest req) {
        return ResponseEntity.ok(productService.updateStock(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
