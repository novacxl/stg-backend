package com.stgian.service;

import com.stgian.dto.DashboardDTOs;
import com.stgian.dto.ProductDTOs;
import com.stgian.model.Product;
import com.stgian.repository.OrderRepository;
import com.stgian.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    private static final int LOW_STOCK_THRESHOLD = 3;

    public DashboardService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDTOs.OverviewResponse getOverview() {
        List<Product> allProducts = productRepository.findByActiveTrue();
        List<Product> lowStock    = productRepository.findByStockLessThanEqualAndActiveTrue(LOW_STOCK_THRESHOLD);
        long totalOrders          = orderRepository.countPaidOrders();
        Long revenue              = orderRepository.totalRevenue();

        List<ProductDTOs.ProductResponse> lowStockDTOs = lowStock.stream()
            .map(ProductDTOs.ProductResponse::from)
            .toList();

        return new DashboardDTOs.OverviewResponse(
            allProducts.size(),
            (int) totalOrders,
            lowStock.size(),
            revenue != null ? revenue : 0L,
            lowStockDTOs
        );
    }
}
