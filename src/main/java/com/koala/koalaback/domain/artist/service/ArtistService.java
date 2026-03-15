package com.koala.koalaback.domain.artist.service;

import com.koala.koalaback.domain.artist.dto.ArtistDto;
import com.koala.koalaback.domain.artist.entity.Artist;
import com.koala.koalaback.domain.artist.repository.ArtistMediaRepository;
import com.koala.koalaback.domain.artist.repository.ArtistRepository;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.global.response.PageResponse;
import com.koala.koalaback.global.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistMediaRepository artistMediaRepository;
    private final CodeGenerator codeGenerator;

    public PageResponse<ArtistDto.SummaryResponse> getArtists(Pageable pageable) {
        return PageResponse.of(
                artistRepository.findByIsActiveTrueAndDeletedAtIsNull(pageable)
                        .map(ArtistDto.SummaryResponse::from)
        );
    }

    public ArtistDto.DetailResponse getArtistBySlug(String slug) {
        Artist artist = artistRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTIST_NOT_FOUND));
        var media = artistMediaRepository.findByArtistIdOrderByMediaRoleAscSortOrderAsc(artist.getId());
        return ArtistDto.DetailResponse.from(artist, media);
    }

    public ArtistDto.DetailResponse getArtistByCode(String artistCode) {
        Artist artist = artistRepository.findByArtistCode(artistCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTIST_NOT_FOUND));
        var media = artistMediaRepository.findByArtistIdOrderByMediaRoleAscSortOrderAsc(artist.getId());
        return ArtistDto.DetailResponse.from(artist, media);
    }

    @Transactional
    public ArtistDto.SummaryResponse createArtist(ArtistDto.CreateRequest req) {
        if (artistRepository.existsBySlug(req.getSlug())) {
            throw new BusinessException(ErrorCode.ARTIST_SLUG_ALREADY_EXISTS);
        }
        Artist artist = Artist.builder()
                .artistCode(codeGenerator.generateCode())
                .name(req.getName())
                .slug(req.getSlug())
                .description(req.getDescription())
                .profileImageUrl(req.getProfileImageUrl())
                .build();
        return ArtistDto.SummaryResponse.from(artistRepository.save(artist));
    }

    @Transactional
    public ArtistDto.SummaryResponse updateArtist(String artistCode, ArtistDto.UpdateRequest req) {
        Artist artist = getArtistEntityByCode(artistCode);

        if (!artist.getSlug().equals(req.getSlug()) && artistRepository.existsBySlug(req.getSlug())) {
            throw new BusinessException(ErrorCode.ARTIST_SLUG_ALREADY_EXISTS);
        }

        artist.update(req.getName(), req.getSlug(), req.getDescription(), req.getProfileImageUrl());
        return ArtistDto.SummaryResponse.from(artist);
    }

    @Transactional
    public void deleteArtist(String artistCode) {
        Artist artist = getArtistEntityByCode(artistCode);
        artist.softDelete();
    }

    // ── Package-level helper (SKU 도메인에서 사용) ─────────

    public Artist getArtistEntityByCode(String artistCode) {
        return artistRepository.findByArtistCode(artistCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTIST_NOT_FOUND));
    }

    public Artist getArtistEntityById(Long id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTIST_NOT_FOUND));
    }
}