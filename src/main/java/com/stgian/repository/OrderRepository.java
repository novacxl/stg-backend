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

    // Receita total (exclui cancelados)
    @Query(value = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE status <> 'CANCELLED'", nativeQuery = true)
    Long totalRevenue();

    // PERF: conta pedidos de um usuário sem carregar os objetos Order inteiros
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    // PERF: soma total gasto por um usuário sem carregar os objetos Order inteiros
    // Evita o N+1 de u.getOrders().stream().mapToInt(total).sum()
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.user.id = :userId AND o.status <> 'CANCELLED'")
    long totalSpentByUserId(@Param("userId") Long userId);
}
