package com.stgian.repository;

import com.stgian.model.StockMovement;
import com.stgian.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductOrderByCreatedAtDesc(Product product);

    List<StockMovement> findAllByOrderByCreatedAtDesc();

    @Query("SELECT m FROM StockMovement m ORDER BY m.createdAt DESC")
    List<StockMovement> findRecent(org.springframework.data.domain.Pageable pageable);

    // Queries originais (mantidas para compatibilidade)
    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m WHERE m.product = :product AND m.type = 'ENTRADA'")
    Integer totalEntradasByProduct(Product product);

    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m WHERE m.product = :product AND m.type IN ('SAIDA','VENDA')")
    Integer totalSaidasByProduct(Product product);

    // FIX-MEDIO-1: Queries agrupadas — substituem N+1 por 2 queries fixas para qualquer volume de produtos
    @Query("SELECT m.product.id, SUM(m.quantity) FROM StockMovement m WHERE m.type = 'ENTRADA' GROUP BY m.product.id")
    List<Object[]> totalEntradasGrouped();

    @Query("SELECT m.product.id, SUM(m.quantity) FROM StockMovement m WHERE m.type IN ('SAIDA','VENDA') GROUP BY m.product.id")
    List<Object[]> totalSaidasGrouped();
}
