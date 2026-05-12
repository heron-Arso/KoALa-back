package com.koala.koalaback.domain.artist.service;

import com.koala.koalaback.domain.artist.dto.ArtistDto;
import com.koala.koalaback.domain.artist.entity.Artist;
import com.koala.koalaback.domain.artist.entity.ArtistFollow;
import com.koala.koalaback.domain.artist.entity.ArtistMedia;
import com.koala.koalaback.domain.artist.repository.ArtistFollowRepository;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistMediaRepository artistMediaRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final CodeGenerator codeGenerator;

    // ── 유저용 ────────────────────────────────────────────

    public PageResponse<ArtistDto.SummaryResponse> getArtists(Pageable pageable) {
        return PageResponse.of(
                artistRepository.findByDeletedAtIsNullAndIsActiveTrue(pageable)
                        .map(ArtistDto.SummaryResponse::from)
        );
    }

    public ArtistDto.DetailResponse getArtist(String artistCode, Long userId) {
        Artist artist = getArtistEntityByCode(artistCode);
        List<ArtistMedia> media = artistMediaRepository
                .findByArtistIdOrderBySortOrderAsc(artist.getId());
        long followCount = artistFollowRepository.countByArtistId(artist.getId());
        boolean isFollowing = userId != null &&
                artistFollowRepository.existsByUserIdAndArtistId(userId, artist.getId());
        return ArtistDto.DetailResponse.from(artist, media, followCount, isFollowing);
    }

    @Transactional
    public void follow(String artistCode, Long userId) {
        Artist artist = getArtistEntityByCode(artistCode);
        if (!artistFollowRepository.existsByUserIdAndArtistId(userId, artist.getId())) {
            artistFollowRepository.save(
                    ArtistFollow.builder().userId(userId).artist(artist).build());
        }
    }

    @Transactional
    public void unfollow(String artistCode, Long userId) {
        Artist artist = getArtistEntityByCode(artistCode);
        artistFollowRepository.findByUserIdAndArtistId(userId, artist.getId())
                .ifPresent(artistFollowRepository::delete);
    }

    // ── 어드민용 ──────────────────────────────────────────

    @Transactional
    public ArtistDto.SummaryResponse createArtist(ArtistDto.CreateRequest req) {
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
        artist.update(req.getName(), req.getSlug(),
                req.getDescription(), req.getProfileImageUrl());
        return ArtistDto.SummaryResponse.from(artist);
    }

    @Transactional
    public void deleteArtist(String artistCode) {
        getArtistEntityByCode(artistCode).softDelete();
    }

    // ── 공통 ──────────────────────────────────────────────

    public Artist getArtistEntityByCode(String artistCode) {
        return artistRepository.findByArtistCode(artistCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}