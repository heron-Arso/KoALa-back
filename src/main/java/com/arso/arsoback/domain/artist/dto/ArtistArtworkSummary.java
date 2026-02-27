package com.arso.arsoback.domain.artist.dto;

import com.arso.arsoback.domain.artwork.entity.Artwork;

public record ArtistArtworkSummary(
        Long id,
        String title,
        Integer year,
        Integer widthMm,
        Integer heightMm,
        Integer price,
        String saleType,
        String status
) {
    public static ArtistArtworkSummary from(Artwork a) {
        return new ArtistArtworkSummary(
                a.getId(),
                a.getTitle(),
                a.getYear(),
                a.getWidthMm(),
                a.getHeightMm(),
                a.getPrice(),
                a.getSaleType(),
                a.getStatus()
        );
    }
}