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

    // CPF armazenado só os dígitos (11 chars), sem pontuação
    @Column(length = 11)
    private String cpf;

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
    protected void onCreate() { this.createdAt = LocalDate.now(); }

    public enum Role { OWNER, ADMIN, CLIENT }

    public User() {}

    public User(Long id, String name, String email, String password, Role role) {
        this.id = id; this.name = name; this.email = email;
        this.password = password; this.role = role;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String name, email, password, cpf;
        private Role role;

        public Builder id(Long v)       { this.id = v; return this; }
        public Builder name(String v)   { this.name = v; return this; }
        public Builder email(String v)  { this.email = v; return this; }
        public Builder password(String v){ this.password = v; return this; }
        public Builder cpf(String v)    { this.cpf = v; return this; }
        public Builder role(Role v)     { this.role = v; return this; }

        public User build() {
            User u = new User();
            u.id = id; u.name = name; u.email = email;
            u.password = password; u.cpf = cpf; u.role = role;
            return u;
        }
    }

    public Long getId()             { return id; }
    public String getName()         { return name; }
    public String getEmail()        { return email; }
    public String getPassword()     { return password; }
    public String getCpf()          { return cpf; }
    public Role getRole()           { return role; }
    public LocalDate getCreatedAt() { return createdAt; }
    public List<Order> getOrders()  { return orders; }
    public List<Product> getWishlist() { return wishlist; }

    public void setId(Long v)           { this.id = v; }
    public void setName(String v)       { this.name = v; }
    public void setEmail(String v)      { this.email = v; }
    public void setPassword(String v)   { this.password = v; }
    public void setCpf(String v)        { this.cpf = v; }
    public void setRole(Role v)         { this.role = v; }
    public void setCreatedAt(LocalDate v){ this.createdAt = v; }
    public void setOrders(List<Order> v){ this.orders = v; }
    public void setWishlist(List<Product> v){ this.wishlist = v; }
}
