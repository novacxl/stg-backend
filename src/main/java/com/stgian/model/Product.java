package com.stgian.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_active",   columnList = "active"),
    @Index(name = "idx_product_category", columnList = "category,active")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    private Badge badge;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(length = 5)
    private String icon;

    // Imagem principal (compatibilidade com versões anteriores)
    @Column(columnDefinition = "LONGTEXT")
    private String imageData;

    // NOVO: galeria de imagens adicionais
    // Cada imagem é armazenada como base64 em linha separada da tabela product_images
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "product_images",
        joinColumns = @JoinColumn(name = "product_id")
    )
    @Column(name = "image_data", columnDefinition = "LONGTEXT")
    @OrderColumn(name = "image_order")
    private List<String> images = new ArrayList<>();

    @Column(nullable = false)
    private Boolean active = true;

    public enum Category { camisetas, moletons, bones }

    public enum Badge {
        new_, hot;
        @Override public String toString() { return this == new_ ? "new" : "hot"; }
    }

    public Product() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String name, description, icon, imageData;
        private Integer price, stock = 0;
        private Category category;
        private Badge badge;
        private Boolean active = true;

        public Builder name(String v)        { this.name = v;        return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder price(Integer v)      { this.price = v;       return this; }
        public Builder category(Category v)  { this.category = v;    return this; }
        public Builder badge(Badge v)        { this.badge = v;       return this; }
        public Builder stock(Integer v)      { this.stock = v;       return this; }
        public Builder icon(String v)        { this.icon = v;        return this; }
        public Builder imageData(String v)   { this.imageData = v;   return this; }
        public Builder active(Boolean v)     { this.active = v;      return this; }

        public Product build() {
            Product p = new Product();
            p.name = name; p.description = description; p.price = price;
            p.category = category; p.badge = badge;
            p.stock = stock != null ? stock : 0;
            p.icon = icon; p.imageData = imageData;
            p.active = active != null ? active : true;
            return p;
        }
    }

    public Long getId()           { return id; }
    public String getName()       { return name; }
    public String getDescription(){ return description; }
    public Integer getPrice()     { return price; }
    public Category getCategory() { return category; }
    public Badge getBadge()       { return badge; }
    public Integer getStock()     { return stock; }
    public String getIcon()       { return icon; }
    public String getImageData()  { return imageData; }
    public List<String> getImages(){ return images; }
    public Boolean getActive()    { return active; }

    public void setId(Long v)              { this.id = v; }
    public void setName(String v)          { this.name = v; }
    public void setDescription(String v)   { this.description = v; }
    public void setPrice(Integer v)        { this.price = v; }
    public void setCategory(Category v)    { this.category = v; }
    public void setBadge(Badge v)          { this.badge = v; }
    public void setStock(Integer v)        { this.stock = v; }
    public void setIcon(String v)          { this.icon = v; }
    public void setImageData(String v)     { this.imageData = v; }
    public void setImages(List<String> v)  { this.images = v; }
    public void setActive(Boolean v)       { this.active = v; }
}
