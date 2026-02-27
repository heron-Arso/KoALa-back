package com.arso.arsoback.domain.artist.service;

import com.arso.arsoback.domain.artist.dto.ArtistArtworkSummary;
import com.arso.arsoback.domain.artist.dto.ArtistChannelResponse;
import com.arso.arsoback.domain.artist.dto.ArtistResponse;
import com.arso.arsoback.domain.artist.entity.Artist;
import com.arso.arsoback.domain.artist.repository.ArtistRepository;
import com.arso.arsoback.domain.artwork.repository.ArtworkRepository;
import com.arso.arsoback.global.exception.BusinessException;
import com.arso.arsoback.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtworkRepository artworkRepository;

    public ArtistChannelResponse getChannel(Long artistId) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "작가를 찾을 수 없습니다. artistId=" + artistId));

        List<ArtistArtworkSummary> artworks = artworkRepository.findAllByArtistIdOrderByCreatedAtDesc(artistId)
                .stream()
                .map(ArtistArtworkSummary::from)
                .toList();

        return ArtistChannelResponse.of(ArtistResponse.from(artist), artworks);
    }
}