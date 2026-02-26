package com.arso.arsoback.domain.product.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String artistName;

    @Column(nullable = false)
    private Integer price; // MVP: 원 단위 int

    @Column(nullable = false)
    private Boolean limited;

    @Column(nullable = true, length = 500)
    private String thumbnailUrl;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.limited == null) this.limited = false;
    }

    public void update(String name, String artistName, Integer price, Boolean limited, String thumbnailUrl) {
        this.name = name;
        this.artistName = artistName;
        this.price = price;
        this.limited = limited;
        this.thumbnailUrl = thumbnailUrl;
    }
}