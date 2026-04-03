package com.stgian.controller;

import com.stgian.dto.ProductDTOs;
import com.stgian.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTOs.ProductResponse>> list(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(productService.findAll(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTOs.ProductResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
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
