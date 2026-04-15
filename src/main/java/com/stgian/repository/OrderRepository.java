package com.stgian.repository;

import com.stgian.model.Order;
import com.stgian.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByCreatedAtDesc(User user);
    Optional<Order> findByOrderCode(String orderCode);

    @Query(value = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE status <> 'CANCELLED'", nativeQuery = true)
    Long totalRevenue();

    // Evita N+1: conta pedidos sem carregar objetos Order
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    // Evita N+1: soma gasto sem carregar objetos Order
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.user.id = :userId AND o.status <> 'CANCELLED'")
    long totalSpentByUserId(@Param("userId") Long userId);
}
