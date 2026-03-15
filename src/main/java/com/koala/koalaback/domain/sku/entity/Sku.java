package com.koala.koalaback.domain.sku.entity;

import com.koala.koalaback.domain.artist.entity.Artist;
import com.koala.koalaback.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "skus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sku extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String skuCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Lob
    private String description;

    @Column(nullable = false, length = 20)
    private String skuType;     // ARTWORK, GOODS

    @Column(nullable = false, length = 50)
    private String genre;       // ART_TOY, ...

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 13, scale = 2)
    private BigDecimal listPrice;

    @Column(precision = 13, scale = 2)
    private BigDecimal salePrice;

    @Column(nullable = false)
    private Boolean isLimitedEdition;

    private Integer editionSize;
    private Integer editionNumber;

    @Column(length = 700)
    private String primaryImageUrl;

    @Column(length = 700)
    private String arAssetUrl;

    @Column(length = 700)
    private String arPreviewImageUrl;

    @Column(columnDefinition = "JSON")
    private String spinePicturesJson;

    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal depthCm;
    private BigDecimal weightKg;

    @Column(nullable = false, length = 20)
    private String status;      // DRAFT, ACTIVE, OUT_OF_STOCK, DISCONTINUED

    private LocalDateTime publishedAt;
    private LocalDateTime deletedAt;

    @Builder
    public Sku(String skuCode, Artist artist, String name, String slug,
               String description, String skuType, String genre,
               String currency, BigDecimal listPrice, BigDecimal salePrice,
               Boolean isLimitedEdition, Integer editionSize, Integer editionNumber,
               String primaryImageUrl, BigDecimal widthCm, BigDecimal heightCm,
               BigDecimal depthCm, BigDecimal weightKg) {
        this.skuCode = skuCode;
        this.artist = artist;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.skuType = skuType != null ? skuType : "ARTWORK";
        this.genre = genre != null ? genre : "ART_TOY";
        this.currency = currency != null ? currency : "KRW";
        this.listPrice = listPrice;
        this.salePrice = salePrice;
        this.isLimitedEdition = isLimitedEdition != null ? isLimitedEdition : false;
        this.editionSize = editionSize;
        this.editionNumber = editionNumber;
        this.primaryImageUrl = primaryImageUrl;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.depthCm = depthCm;
        this.weightKg = weightKg;
        this.status = "DRAFT";
    }

    public void update(String name, String slug, String description,
                       BigDecimal listPrice, BigDecimal salePrice, String primaryImageUrl) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.listPrice = listPrice;
        this.salePrice = salePrice;
        this.primaryImageUrl = primaryImageUrl;
    }

    public void publish() {
        this.status = "ACTIVE";
        this.publishedAt = LocalDateTime.now();
    }

    public void discontinue() {
        this.status = "DISCONTINUED";
    }

    public void markOutOfStock() {
        this.status = "OUT_OF_STOCK";
    }

    public void markActive() {
        this.status = "ACTIVE";
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = "DISCONTINUED";
    }

    public boolean isAvailable() {
        return "ACTIVE".equals(this.status) && this.deletedAt == null;
    }

    public BigDecimal getEffectivePrice() {
        return salePrice != null ? salePrice : listPrice;
    }
}