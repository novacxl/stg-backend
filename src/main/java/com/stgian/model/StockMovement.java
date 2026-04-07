package com.stgian.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "stock_before", nullable = false)
    private Integer stockBefore;

    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @Column(length = 500)
    private String reason;

    // Usuário ADM/OWNER que registrou (null = sistema automático)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @Column(name = "order_code")
    private String orderCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    public enum Type {
        ENTRADA,  // reposição manual ou devolução
        SAIDA,    // saída manual (perda, descarte, ajuste)
        VENDA     // saída automática por compra de cliente
    }

    public StockMovement() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Product product; private Type type; private Integer quantity;
        private Integer stockBefore; private Integer stockAfter;
        private String reason; private User registeredBy; private String orderCode;

        public Builder product(Product v)     { this.product = v; return this; }
        public Builder type(Type v)           { this.type = v; return this; }
        public Builder quantity(Integer v)    { this.quantity = v; return this; }
        public Builder stockBefore(Integer v) { this.stockBefore = v; return this; }
        public Builder stockAfter(Integer v)  { this.stockAfter = v; return this; }
        public Builder reason(String v)       { this.reason = v; return this; }
        public Builder registeredBy(User v)   { this.registeredBy = v; return this; }
        public Builder orderCode(String v)    { this.orderCode = v; return this; }

        public StockMovement build() {
            StockMovement m = new StockMovement();
            m.product = product; m.type = type; m.quantity = quantity;
            m.stockBefore = stockBefore; m.stockAfter = stockAfter;
            m.reason = reason; m.registeredBy = registeredBy; m.orderCode = orderCode;
            return m;
        }
    }

    // Getters
    public Long getId()              { return id; }
    public Product getProduct()      { return product; }
    public Type getType()            { return type; }
    public Integer getQuantity()     { return quantity; }
    public Integer getStockBefore()  { return stockBefore; }
    public Integer getStockAfter()   { return stockAfter; }
    public String getReason()        { return reason; }
    public User getRegisteredBy()    { return registeredBy; }
    public String getOrderCode()     { return orderCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long v)              { this.id = v; }
    public void setProduct(Product v)      { this.product = v; }
    public void setType(Type v)            { this.type = v; }
    public void setQuantity(Integer v)     { this.quantity = v; }
    public void setStockBefore(Integer v)  { this.stockBefore = v; }
    public void setStockAfter(Integer v)   { this.stockAfter = v; }
    public void setReason(String v)        { this.reason = v; }
    public void setRegisteredBy(User v)    { this.registeredBy = v; }
    public void setOrderCode(String v)     { this.orderCode = v; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
