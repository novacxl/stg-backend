package com.stgian.model;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_code",      columnList = "order_code"),
    @Index(name = "idx_order_user",      columnList = "user_id"),
    @Index(name = "idx_order_status",    columnList = "status"),
    @Index(name = "idx_order_created",   columnList = "created_at")
})
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private Integer total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING_PAYMENT;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ── Endereço de entrega ──
    @Column(name = "shipping_name")          private String shippingName;
    @Column(name = "shipping_email")         private String shippingEmail;
    @Column(name = "shipping_phone")         private String shippingPhone;
    @Column(name = "shipping_cep")           private String shippingCep;
    @Column(name = "shipping_street")        private String shippingStreet;
    @Column(name = "shipping_number")        private String shippingNumber;
    @Column(name = "shipping_complement")    private String shippingComplement;
    @Column(name = "shipping_neighborhood")  private String shippingNeighborhood;
    @Column(name = "shipping_city")          private String shippingCity;
    @Column(name = "shipping_state", length = 2) private String shippingState;

    // ── Pagamento ──
    @Column(name = "payment_method")         private String paymentMethod;
    @Column(name = "payment_status")         private String paymentStatus;
    @Column(name = "mp_preference_id")       private String mpPreferenceId;
    @Column(name = "mp_payment_id")          private String mpPaymentId;
    @Column(name = "mp_checkout_url", length = 1000) private String mpCheckoutUrl;

    // ── Rastreio ──
    @Column(name = "tracking_code",    length = 50)  private String trackingCode;
    @Column(name = "tracking_carrier", length = 50)  private String trackingCarrier;
    @Column(name = "tracking_url",     length = 500) private String trackingUrl;
    @Column(name = "tracking_status",  length = 200) private String trackingStatus;
    @Column(name = "tracking_updated_at")            private LocalDateTime trackingUpdatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.orderCode == null)
            // FIX-BAIXO-2: UUID garante unicidade — Math.random() tem apenas 8999 valores possíveis
            this.orderCode = "STG-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        if (this.paymentStatus == null)
            this.paymentStatus = "PENDING";
    }

    public enum Status {
        PENDING_PAYMENT,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }

    public Order() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User user; private Integer total; private Status status = Status.PENDING_PAYMENT;
        private String shippingName, shippingEmail, shippingPhone, shippingCep;
        private String shippingStreet, shippingNumber, shippingComplement;
        private String shippingNeighborhood, shippingCity, shippingState;
        private String paymentMethod;

        public Builder user(User v)                   { this.user = v; return this; }
        public Builder total(Integer v)               { this.total = v; return this; }
        public Builder status(Status v)               { this.status = v; return this; }
        public Builder shippingName(String v)         { this.shippingName = v; return this; }
        public Builder shippingEmail(String v)        { this.shippingEmail = v; return this; }
        public Builder shippingPhone(String v)        { this.shippingPhone = v; return this; }
        public Builder shippingCep(String v)          { this.shippingCep = v; return this; }
        public Builder shippingStreet(String v)       { this.shippingStreet = v; return this; }
        public Builder shippingNumber(String v)       { this.shippingNumber = v; return this; }
        public Builder shippingComplement(String v)   { this.shippingComplement = v; return this; }
        public Builder shippingNeighborhood(String v) { this.shippingNeighborhood = v; return this; }
        public Builder shippingCity(String v)         { this.shippingCity = v; return this; }
        public Builder shippingState(String v)        { this.shippingState = v; return this; }
        public Builder paymentMethod(String v)        { this.paymentMethod = v; return this; }

        public Order build() {
            Order o = new Order();
            o.user = user; o.total = total;
            o.status = status != null ? status : Status.PENDING_PAYMENT;
            o.shippingName = shippingName; o.shippingEmail = shippingEmail;
            o.shippingPhone = shippingPhone; o.shippingCep = shippingCep;
            o.shippingStreet = shippingStreet; o.shippingNumber = shippingNumber;
            o.shippingComplement = shippingComplement;
            o.shippingNeighborhood = shippingNeighborhood;
            o.shippingCity = shippingCity; o.shippingState = shippingState;
            o.paymentMethod = paymentMethod;
            return o;
        }
    }

    // ── Getters ──
    public Long getId()                   { return id; }
    public String getOrderCode()          { return orderCode; }
    public User getUser()                 { return user; }
    public List<OrderItem> getItems()     { return items; }
    public Integer getTotal()             { return total; }
    public Status getStatus()             { return status; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public String getShippingName()       { return shippingName; }
    public String getShippingEmail()      { return shippingEmail; }
    public String getShippingPhone()      { return shippingPhone; }
    public String getShippingCep()        { return shippingCep; }
    public String getShippingStreet()     { return shippingStreet; }
    public String getShippingNumber()     { return shippingNumber; }
    public String getShippingComplement() { return shippingComplement; }
    public String getShippingNeighborhood(){ return shippingNeighborhood; }
    public String getShippingCity()       { return shippingCity; }
    public String getShippingState()      { return shippingState; }
    public String getPaymentMethod()      { return paymentMethod; }
    public String getPaymentStatus()      { return paymentStatus; }
    public String getMpPreferenceId()     { return mpPreferenceId; }
    public String getMpPaymentId()        { return mpPaymentId; }
    public String getMpCheckoutUrl()      { return mpCheckoutUrl; }
    public String getTrackingCode()       { return trackingCode; }
    public String getTrackingCarrier()    { return trackingCarrier; }
    public String getTrackingUrl()        { return trackingUrl; }
    public String getTrackingStatus()     { return trackingStatus; }
    public LocalDateTime getTrackingUpdatedAt() { return trackingUpdatedAt; }

    // ── Setters ──
    public void setId(Long v)                    { this.id = v; }
    public void setOrderCode(String v)           { this.orderCode = v; }
    public void setUser(User v)                  { this.user = v; }
    public void setItems(List<OrderItem> v)      { this.items = v; }
    public void setTotal(Integer v)              { this.total = v; }
    public void setStatus(Status v)              { this.status = v; }
    public void setCreatedAt(LocalDateTime v)    { this.createdAt = v; }
    public void setShippingName(String v)        { this.shippingName = v; }
    public void setShippingEmail(String v)       { this.shippingEmail = v; }
    public void setShippingPhone(String v)       { this.shippingPhone = v; }
    public void setShippingCep(String v)         { this.shippingCep = v; }
    public void setShippingStreet(String v)      { this.shippingStreet = v; }
    public void setShippingNumber(String v)      { this.shippingNumber = v; }
    public void setShippingComplement(String v)  { this.shippingComplement = v; }
    public void setShippingNeighborhood(String v){ this.shippingNeighborhood = v; }
    public void setShippingCity(String v)        { this.shippingCity = v; }
    public void setShippingState(String v)       { this.shippingState = v; }
    public void setPaymentMethod(String v)       { this.paymentMethod = v; }
    public void setPaymentStatus(String v)       { this.paymentStatus = v; }
    public void setMpPreferenceId(String v)      { this.mpPreferenceId = v; }
    public void setMpPaymentId(String v)         { this.mpPaymentId = v; }
    public void setMpCheckoutUrl(String v)       { this.mpCheckoutUrl = v; }
    public void setTrackingCode(String v)        { this.trackingCode = v; }
    public void setTrackingCarrier(String v)     { this.trackingCarrier = v; }
    public void setTrackingUrl(String v)         { this.trackingUrl = v; }
    public void setTrackingStatus(String v)      { this.trackingStatus = v; }
    public void setTrackingUpdatedAt(LocalDateTime v) { this.trackingUpdatedAt = v; }
}
