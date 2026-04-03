package com.stgian.repository;

import com.stgian.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();
    List<Product> findByCategoryAndActiveTrue(Product.Category category);
    List<Product> findByStockLessThanEqualAndActiveTrue(int threshold);

    // FIX-CRITICO-2: Lock pessimista para evitar race condition no checkout
    // Quando dois clientes compram o mesmo produto ao mesmo tempo,
    // o segundo fica aguardando o primeiro terminar antes de ler o estoque
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);
}
