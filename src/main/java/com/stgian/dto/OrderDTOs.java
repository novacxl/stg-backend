package com.stgian.dto;

import com.stgian.model.Order;
import com.stgian.model.OrderItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class OrderDTOs {

    public record OrderItemRequest(
        @NotNull Long productId,
        @NotBlank String size,
        @Min(1) int quantity
    ) {}

    public record AddressRequest(
        @NotBlank String name,
        @NotBlank String phone,
        @NotBlank String cep,
        @NotBlank String street,
        @NotBlank String number,
        String comp,
        @NotBlank String hood,
        @NotBlank String city,
        @NotBlank String state
    ) {}

    public record CheckoutRequest(
        @NotEmpty @Size(max = 50, message = "Máximo de 50 itens por pedido") List<OrderItemRequest> items,
        @NotNull AddressRequest address,
        String paymentMethod
    ) {}

    public record StatusUpdateRequest(@NotBlank String status) {}

    public record OrderItemResponse(
        Long productId,
        String productName,
        String size,
        int quantity,
        int unitPrice,
        int subtotal
    ) {
        public static OrderItemResponse from(OrderItem i) {
            return new OrderItemResponse(
                i.getProduct().getId(), i.getProduct().getName(),
                i.getSize(), i.getQuantity(),
                i.getUnitPrice(), i.getUnitPrice() * i.getQuantity()
            );
        }
    }

    public record ShippingResponse(
        String name, String phone, String cep,
        String street, String number, String complement,
        String neighborhood, String city, String state
    ) {}

    public record TrackingInfo(
        String code,
        String carrier,
        String url,
        String status,
        LocalDateTime updatedAt
    ) {}

    public record OrderResponse(
        Long id,
        String orderCode,
        String status,
        String paymentStatus,
        String paymentMethod,
        String mpCheckoutUrl,
        String mpPreferenceId,
        int total,
        LocalDateTime createdAt,
        String userName,
        String userEmail,
        ShippingResponse shipping,
        TrackingInfo tracking,
        List<OrderItemResponse> items
    ) {
        public static OrderResponse from(Order o) {
            List<OrderItemResponse> itemDTOs = o.getItems().stream()
                .map(OrderItemResponse::from).toList();

            ShippingResponse shipping = new ShippingResponse(
                o.getShippingName(), o.getShippingPhone(), o.getShippingCep(),
                o.getShippingStreet(), o.getShippingNumber(), o.getShippingComplement(),
                o.getShippingNeighborhood(), o.getShippingCity(), o.getShippingState()
            );

            TrackingInfo tracking = new TrackingInfo(
                o.getTrackingCode(), o.getTrackingCarrier(),
                o.getTrackingUrl(), o.getTrackingStatus(),
                o.getTrackingUpdatedAt()
            );

            return new OrderResponse(
                o.getId(), o.getOrderCode(), o.getStatus().name(),
                o.getPaymentStatus(), o.getPaymentMethod(),
                o.getMpCheckoutUrl(), o.getMpPreferenceId(),
                o.getTotal(), o.getCreatedAt(),
                o.getUser().getName(), o.getUser().getEmail(),
                shipping, tracking, itemDTOs
            );
        }
    }
}
