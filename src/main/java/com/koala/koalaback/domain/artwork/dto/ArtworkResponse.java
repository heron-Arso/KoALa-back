package com.koala.koalaback.domain.artwork.dto;

import com.koala.koalaback.domain.artwork.entity.Artwork;

import java.time.LocalDateTime;

public record ArtworkResponse(
        Long id,
        Long artistId,
        String title,
        String description,
        Integer year,
        Integer widthMm,
        Integer heightMm,
        Integer price,
        String saleType,
        String status,
        LocalDateTime createdAt
) {
    public static ArtworkResponse from(Artwork a) {
        return new ArtworkResponse(
                a.getId(),
                a.getArtistId(),
                a.getTitle(),
                a.getDescription(),
                a.getYear(),
                a.getWidthMm(),
                a.getHeightMm(),
                a.getPrice(),
                a.getSaleType(),
                a.getStatus(),
                a.getCreatedAt()
        );
    }
}