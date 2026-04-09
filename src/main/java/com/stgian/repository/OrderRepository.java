package com.stgian.repository;

import com.stgian.model.Order;
import com.stgian.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    Optional<Order> findByOrderCode(String orderCode);

    // nativeQuery=true: única forma que funciona com Hibernate 6 + enum como string no banco
    // Receita real: só pedidos com pagamento APROVADO e não cancelados
    @Query(value = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE payment_status = 'APPROVED' AND status <> 'CANCELLED'", nativeQuery = true)
    Long totalRevenue();

    // Contagem só de pedidos com pagamento real (excluindo pendentes e cancelados)
    @Query(value = "SELECT COUNT(*) FROM orders WHERE payment_status = 'APPROVED'", nativeQuery = true)
    Long countPaidOrders();
}
