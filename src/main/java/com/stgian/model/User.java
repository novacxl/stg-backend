package com.stgian.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_wishlist",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<Product> wishlist = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
    }

    public enum Role { OWNER, ADMIN, CLIENT }

    // ── Construtores ──
    public User() {}

    public User(Long id, String name, String email, String password, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // ── Builder estático ──
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String name;
        private String email;
        private String password;
        private Role role;

        public Builder id(Long id)           { this.id = id; return this; }
        public Builder name(String name)     { this.name = name; return this; }
        public Builder email(String email)   { this.email = email; return this; }
        public Builder password(String pw)   { this.password = pw; return this; }
        public Builder role(Role role)       { this.role = role; return this; }

        public User build() {
            User u = new User();
            u.id = this.id;
            u.name = this.name;
            u.email = this.email;
            u.password = this.password;
            u.role = this.role;
            return u;
        }
    }

    // ── Getters ──
    public Long getId()            { return id; }
    public String getName()        { return name; }
    public String getEmail()       { return email; }
    public String getPassword()    { return password; }
    public Role getRole()          { return role; }
    public LocalDate getCreatedAt(){ return createdAt; }
    public List<Order> getOrders() { return orders; }
    public List<Product> getWishlist() { return wishlist; }

    // ── Setters ──
    public void setId(Long id)           { this.id = id; }
    public void setName(String name)     { this.name = name; }
    public void setEmail(String email)   { this.email = email; }
    public void setPassword(String pw)   { this.password = pw; }
    public void setRole(Role role)       { this.role = role; }
    public void setCreatedAt(LocalDate d){ this.createdAt = d; }
    public void setOrders(List<Order> o) { this.orders = o; }
    public void setWishlist(List<Product> w) { this.wishlist = w; }
}
