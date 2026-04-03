package com.stgian.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 10)
    private String size;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    // ── Construtores ──
    public OrderItem() {}

    // ── Builder ──
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Order order;
        private Product product;
        private String size;
        private Integer quantity;
        private Integer unitPrice;

        public Builder order(Order v)      { this.order = v; return this; }
        public Builder product(Product v)  { this.product = v; return this; }
        public Builder size(String v)      { this.size = v; return this; }
        public Builder quantity(Integer v) { this.quantity = v; return this; }
        public Builder unitPrice(Integer v){ this.unitPrice = v; return this; }

        public OrderItem build() {
            OrderItem i = new OrderItem();
            i.order = this.order;
            i.product = this.product;
            i.size = this.size;
            i.quantity = this.quantity;
            i.unitPrice = this.unitPrice;
            return i;
        }
    }

    // ── Getters ──
    public Long getId()         { return id; }
    public Order getOrder()     { return order; }
    public Product getProduct() { return product; }
    public String getSize()     { return size; }
    public Integer getQuantity(){ return quantity; }
    public Integer getUnitPrice(){ return unitPrice; }

    // ── Setters ──
    public void setId(Long v)          { this.id = v; }
    public void setOrder(Order v)      { this.order = v; }
    public void setProduct(Product v)  { this.product = v; }
    public void setSize(String v)      { this.size = v; }
    public void setQuantity(Integer v) { this.quantity = v; }
    public void setUnitPrice(Integer v){ this.unitPrice = v; }
}
