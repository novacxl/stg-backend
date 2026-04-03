package com.stgian.repository;

import com.stgian.model.Product;
import com.stgian.model.StockMovement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByProductOrderByCreatedAtDesc(Product product);
    List<StockMovement> findAllByOrderByCreatedAtDesc();

    @Query("SELECT m FROM StockMovement m ORDER BY m.createdAt DESC")
    List<StockMovement> findRecentMovements(Pageable pageable);
}
