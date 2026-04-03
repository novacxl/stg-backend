package com.stgian.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "drops")
public class Drop {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Boolean active = true;
    @Column(nullable = false) private String tag;
    @Column(name = "title1", nullable = false) private String title1;
    @Column(name = "title2") private String title2;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "bg_num", length = 5) private String bgNum;
    @Column(name = "launch_date") private LocalDateTime launchDate;
    @Column(name = "mini1_name") private String mini1Name;
    @Column(name = "mini1_price") private Integer mini1Price;
    @Column(name = "mini1_desc") private String mini1Desc;
    @Column(name = "mini1_tag") private String mini1Tag;
    @Column(name = "mini2_name") private String mini2Name;
    @Column(name = "mini2_price") private Integer mini2Price;
    @Column(name = "mini2_desc") private String mini2Desc;
    @Column(name = "mini2_tag") private String mini2Tag;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { this.createdAt = LocalDateTime.now(); this.updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
    public Drop() {}
    public Long getId() { return id; } public Boolean getActive() { return active; } public String getTag() { return tag; }
    public String getTitle1() { return title1; } public String getTitle2() { return title2; } public String getDescription() { return description; }
    public String getBgNum() { return bgNum; } public LocalDateTime getLaunchDate() { return launchDate; }
    public String getMini1Name() { return mini1Name; } public Integer getMini1Price() { return mini1Price; }
    public String getMini1Desc() { return mini1Desc; } public String getMini1Tag() { return mini1Tag; }
    public String getMini2Name() { return mini2Name; } public Integer getMini2Price() { return mini2Price; }
    public String getMini2Desc() { return mini2Desc; } public String getMini2Tag() { return mini2Tag; }
    public LocalDateTime getCreatedAt() { return createdAt; } public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setId(Long v) { this.id = v; } public void setActive(Boolean v) { this.active = v; } public void setTag(String v) { this.tag = v; }
    public void setTitle1(String v) { this.title1 = v; } public void setTitle2(String v) { this.title2 = v; } public void setDescription(String v) { this.description = v; }
    public void setBgNum(String v) { this.bgNum = v; } public void setLaunchDate(LocalDateTime v) { this.launchDate = v; }
    public void setMini1Name(String v) { this.mini1Name = v; } public void setMini1Price(Integer v) { this.mini1Price = v; }
    public void setMini1Desc(String v) { this.mini1Desc = v; } public void setMini1Tag(String v) { this.mini1Tag = v; }
    public void setMini2Name(String v) { this.mini2Name = v; } public void setMini2Price(Integer v) { this.mini2Price = v; }
    public void setMini2Desc(String v) { this.mini2Desc = v; } public void setMini2Tag(String v) { this.mini2Tag = v; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; } public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
