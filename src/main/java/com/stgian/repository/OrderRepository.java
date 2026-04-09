package com.stgian.repository;

import com.stgian.model.Order;
import com.stgian.model.User;
import org.springframework.data.domain.Pageable;
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

    // FIX: adicionados countByUser e sumTotalByUser que o UserService precisa
    @Query(value = "SELECT COUNT(*) FROM orders WHERE user_id = :#{#user.id}", nativeQuery = true)
    int countByUser(@Param("user") User user);

    @Query(value = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE user_id = :#{#user.id} AND payment_status = 'APPROVED'", nativeQuery = true)
    Long sumTotalByUser(@Param("user") User user);

    // Receita total (dashboard)
    @Query(value = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE payment_status = 'APPROVED' AND status <> 'CANCELLED'", nativeQuery = true)
    Long totalRevenue();

    // Contagem de pedidos pagos (dashboard)
    @Query(value = "SELECT COUNT(*) FROM orders WHERE payment_status = 'APPROVED'", nativeQuery = true)
    Long countPaidOrders();
}
