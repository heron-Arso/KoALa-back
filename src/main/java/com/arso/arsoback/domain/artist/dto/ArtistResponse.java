package com.arso.arsoback.domain.artist.dto;

import com.arso.arsoback.domain.artist.entity.Artist;

import java.time.LocalDateTime;

public record ArtistResponse(
        Long id,
        String displayName,
        String bio,
        String profileImageUrl,
        LocalDateTime createdAt
) {
    public static ArtistResponse from(Artist a) {
        return new ArtistResponse(
                a.getId(),
                a.getDisplayName(),
                a.getBio(),
                a.getProfileImageUrl(),
                a.getCreatedAt()
        );
    }
}