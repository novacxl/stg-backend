package com.stgian.dto;

import java.util.List;

public class DashboardDTOs {

    public record OverviewResponse(
        int totalProducts,
        int totalOrders,
        int lowStockCount,
        long totalRevenue,
        List<ProductDTOs.ProductResponse> lowStockProducts
    ) {}
}
