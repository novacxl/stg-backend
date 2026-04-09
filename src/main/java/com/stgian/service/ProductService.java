package com.stgian.service;

import com.stgian.dto.ProductDTOs;
import com.stgian.model.Product;
import com.stgian.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDTOs.ProductSummary> findAll(String category) {
        List<Product> products;
        if (category != null && !category.isBlank()) {
            try {
                Product.Category cat = Product.Category.valueOf(category.toLowerCase());
                products = productRepository.findByCategoryAndActiveTrue(cat);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Categoria inválida: " + category);
            }
        } else {
            products = productRepository.findByActiveTrue();
        }
        return products.stream().map(ProductDTOs.ProductSummary::from).toList();
    }

    public ProductDTOs.ProductResponse findById(Long id) {
        return ProductDTOs.ProductResponse.from(getOrThrow(id));
    }

    public ProductDTOs.ProductResponse create(ProductDTOs.ProductRequest req) {
        Product p = Product.builder()
            .name(req.name()).description(req.description()).price(req.price())
            .category(parseCategory(req.category())).badge(parseBadge(req.badge()))
            .stock(req.stock() != null ? req.stock() : 0)
            .icon(req.icon() != null ? req.icon() : req.name().substring(0, 1).toUpperCase())
            .imageData(req.imageData())
            .build();
        if (req.images() != null) p.setImages(req.images());
        return ProductDTOs.ProductResponse.from(productRepository.save(p));
    }

    public ProductDTOs.ProductResponse update(Long id, ProductDTOs.ProductRequest req) {
        Product p = getOrThrow(id);
        p.setName(req.name()); p.setDescription(req.description()); p.setPrice(req.price());
        p.setCategory(parseCategory(req.category())); p.setBadge(parseBadge(req.badge()));
        if (req.stock()     != null) p.setStock(req.stock());
        if (req.icon()      != null) p.setIcon(req.icon());
        if (req.imageData() != null) p.setImageData(req.imageData());
        // Galeria: null = não altera, lista vazia = limpa, lista com itens = substitui
        if (req.images() != null)    p.setImages(req.images());
        return ProductDTOs.ProductResponse.from(productRepository.save(p));
    }

    public ProductDTOs.ProductResponse updateStock(Long id, ProductDTOs.StockUpdateRequest req) {
        Product p = getOrThrow(id);
        p.setStock(req.stock());
        return ProductDTOs.ProductResponse.from(productRepository.save(p));
    }

    public void delete(Long id) {
        Product p = getOrThrow(id);
        p.setActive(false);
        productRepository.save(p);
    }

    private Product getOrThrow(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Produto nao encontrado: " + id));
    }

    private Product.Category parseCategory(String cat) {
        try { return Product.Category.valueOf(cat.toLowerCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Categoria inválida: " + cat); }
    }

    private Product.Badge parseBadge(String badge) {
        if (badge == null || badge.isBlank()) return null;
        return switch (badge) {
            case "new" -> Product.Badge.new_;
            case "hot" -> Product.Badge.hot;
            default    -> null;
        };
    }
}
