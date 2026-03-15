package com.koala.koalaback.domain.artist.entity;

import com.koala.koalaback.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "artists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artist extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String artistCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Lob
    private String description;

    @Column(length = 700)
    private String profileImageUrl;

    @Column(nullable = false)
    private Boolean isActive;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ArtistMedia> mediaList = new ArrayList<>();

    @Builder
    public Artist(String artistCode, String name, String slug,
                  String description, String profileImageUrl) {
        this.artistCode = artistCode;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
        this.isActive = true;
    }

    public void update(String name, String slug, String description, String profileImageUrl) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
    }

    public void activate() { this.isActive = true; }
    public void deactivate() { this.isActive = false; }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.isActive = false;
    }
}