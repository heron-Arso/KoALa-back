package com.arso.arsoback.domain.artist.dto;

import java.util.List;

public record ArtistChannelResponse(
        ArtistResponse artist,
        List<ArtistArtworkSummary> artworks
) {
    public static ArtistChannelResponse of(ArtistResponse artist, List<ArtistArtworkSummary> artworks) {
        return new ArtistChannelResponse(artist, artworks);
    }
}