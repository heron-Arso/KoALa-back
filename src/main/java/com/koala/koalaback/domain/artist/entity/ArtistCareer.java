package com.koala.koalaback.domain.artist.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "artist_careers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistCareer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Column(nullable = false, length = 20)
    private String category;   // 학력 | 개인전 | 그룹전

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private Integer sortOrder;

    private LocalDateTime createdAt;

    @Builder
    public ArtistCareer(Artist artist, String category,
                        Integer year, String content, Integer sortOrder) {
        this.artist    = artist;
        this.category  = category;
        this.year      = year;
        this.content   = content;
        this.sortOrder = sortOrder;
        this.createdAt = LocalDateTime.now();
    }
}