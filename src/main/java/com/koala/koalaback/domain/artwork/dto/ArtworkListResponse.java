package com.koala.koalaback.domain.artwork.dto;

import com.koala.koalaback.domain.artwork.entity.Artwork;

public record ArtworkListResponse(
        Long id,
        String title,
        Integer year,
        Integer price,
        String saleType,
        String status
) {
    public static ArtworkListResponse from(Artwork a) {
        return new ArtworkListResponse(
                a.getId(),
                a.getTitle(),
                a.getYear(),
                a.getPrice(),
                a.getSaleType(),
                a.getStatus()
        );
    }
}