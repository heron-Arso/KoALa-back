package com.koala.koalaback.domain.artist.service;

import com.koala.koalaback.domain.artist.dto.ArtistDto;
import com.koala.koalaback.domain.artist.entity.Artist;
import com.koala.koalaback.domain.artist.entity.ArtistCareer;
import com.koala.koalaback.domain.artist.entity.ArtistFollow;
import com.koala.koalaback.domain.artist.entity.ArtistMedia;
import com.koala.koalaback.domain.artist.repository.ArtistCareerRepository;
import com.koala.koalaback.domain.artist.repository.ArtistFollowRepository;
import com.koala.koalaback.domain.artist.repository.ArtistMediaRepository;
import com.koala.koalaback.domain.artist.repository.ArtistRepository;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.global.response.PageResponse;
import com.koala.koalaback.global.util.CodeGenerator;
import com.koala.koalaback.infra.storage.StorageUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistMediaRepository artistMediaRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistCareerRepository artistCareerRepository;
    private final StorageUploader s3Uploader;
    private final CodeGenerator codeGenerator;

    // ── 유저용 ────────────────────────────────────────────

    public PageResponse<ArtistDto.SummaryResponse> getArtists(Pageable pageable) {
        Page<Artist> page = artistRepository.findByDeletedAtIsNullAndIsActiveTrue(pageable);
        List<Long> ids = page.getContent().stream().map(Artist::getId).toList();

        if (ids.isEmpty()) {
            return PageResponse.of(page.map(ArtistDto.SummaryResponse::from));
        }

        // 미디어 배치 로드 (N+1 방지)
        Map<Long, List<ArtistMedia>> mediaByArtist = artistMediaRepository
                .findByArtistIdIn(ids)
                .stream()
                .collect(Collectors.groupingBy(m -> m.getArtist().getId()));

        // 팔로워 수 배치 로드 (N+1 방지)
        Map<Long, Long> followByArtist = artistFollowRepository
                .countsByArtistIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        ArtistFollowRepository.FollowCountProjection::getArtistId,
                        ArtistFollowRepository.FollowCountProjection::getCnt
                ));

        return PageResponse.of(page.map(a -> ArtistDto.SummaryResponse.fromWithMedia(
                a,
                mediaByArtist.getOrDefault(a.getId(), List.of()),
                followByArtist.getOrDefault(a.getId(), 0L)
        )));
    }

    public ArtistDto.DetailResponse getArtist(String artistCode, Long userId) {
        Artist artist = getArtistEntityByCode(artistCode);
        List<ArtistMedia> media = artistMediaRepository
                .findByArtistIdOrderBySortOrderAsc(artist.getId());
        List<ArtistCareer> careers = artistCareerRepository
                .findByArtistIdOrderByCategoryAscSortOrderAsc(artist.getId());
        long followCount = artistFollowRepository.countByArtistId(artist.getId());
        boolean isFollowing = userId != null &&
                artistFollowRepository.existsByUserIdAndArtistId(userId, artist.getId());
        return ArtistDto.DetailResponse.from(artist, media, careers, followCount, isFollowing);
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
                .artistNote(req.getArtistNote())
                .profileImageUrl(req.getProfileImageUrl())
                .build();
        return ArtistDto.SummaryResponse.from(artistRepository.save(artist));
    }

    @Transactional
    public ArtistDto.SummaryResponse updateArtist(String artistCode, ArtistDto.UpdateRequest req) {
        Artist artist = getArtistEntityByCode(artistCode);
        artist.update(req.getName(), req.getSlug(),
                req.getDescription(), req.getArtistNote(), req.getProfileImageUrl());
        return ArtistDto.SummaryResponse.from(artist);
    }

    @Transactional
    public void deleteArtist(String artistCode) {
        getArtistEntityByCode(artistCode).softDelete();
    }

    // ── 미디어 관리 (어드민) ──────────────────────────────

    @Transactional
    public ArtistDto.MediaResponse addMedia(String artistCode,
                                            MultipartFile file,
                                            ArtistDto.MediaAddRequest req) {
        Artist artist = getArtistEntityByCode(artistCode);
        String dir = "artists/" + artist.getArtistCode() + "/" + req.getMediaRole().toLowerCase();
        String fileUrl = s3Uploader.upload(file, dir);

        int nextOrder = req.getSortOrder() != null ? req.getSortOrder()
                : artistMediaRepository.findByArtistIdOrderBySortOrderAsc(artist.getId()).size();

        ArtistMedia media = ArtistMedia.builder()
                .artist(artist)
                .mediaType(req.getMediaType())
                .mediaRole(req.getMediaRole())
                .fileUrl(fileUrl)
                .title(req.getTitle())
                .sortOrder(nextOrder)
                .build();
        artistMediaRepository.save(media);

        // 프로필 사진 업로드 시 artist.profileImageUrl 자동 동기화
        if ("PROFILE".equals(req.getMediaRole())) {
            artist.updateProfileImage(fileUrl);
        }

        return ArtistDto.MediaResponse.from(media);
    }

    /** YouTube 등 외부 URL을 파일 업로드 없이 미디어로 등록 */
    @Transactional
    public ArtistDto.MediaResponse addMediaUrl(String artistCode, ArtistDto.MediaUrlRequest req) {
        Artist artist = getArtistEntityByCode(artistCode);

        // 같은 role의 기존 미디어를 한 번에 삭제 (인터뷰 영상은 1개만 유지)
        artistMediaRepository.deleteByArtistIdAndMediaRole(artist.getId(), req.getMediaRole());

        int order = req.getSortOrder() != null ? req.getSortOrder() : 0;
        ArtistMedia media = ArtistMedia.builder()
                .artist(artist)
                .mediaType(req.getMediaType())
                .mediaRole(req.getMediaRole())
                .fileUrl(req.getFileUrl())
                .title(req.getTitle())
                .sortOrder(order)
                .build();
        artistMediaRepository.save(media);
        return ArtistDto.MediaResponse.from(media);
    }

    @Transactional
    public void deleteMedia(String artistCode, Long mediaId) {
        Artist artist = getArtistEntityByCode(artistCode);
        ArtistMedia media = artistMediaRepository.findById(mediaId)
                .filter(m -> m.getArtist().getId().equals(artist.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        s3Uploader.delete(media.getFileUrl());
        artistMediaRepository.delete(media);
    }

    public List<ArtistDto.MediaResponse> getMediaList(String artistCode) {
        Artist artist = getArtistEntityByCode(artistCode);
        return artistMediaRepository
                .findByArtistIdOrderBySortOrderAsc(artist.getId())
                .stream().map(ArtistDto.MediaResponse::from).toList();
    }

    // ── 약력 관리 (어드민) ────────────────────────────────

    @Transactional
    public ArtistDto.CareerResponse addCareer(String artistCode, ArtistDto.CareerAddRequest req) {
        Artist artist = getArtistEntityByCode(artistCode);
        int nextOrder = req.getSortOrder() != null ? req.getSortOrder()
                : artistCareerRepository.findByArtistIdOrderByCategoryAscSortOrderAsc(artist.getId()).size();

        ArtistCareer career = ArtistCareer.builder()
                .artist(artist)
                .category(req.getCategory())
                .year(req.getYear())
                .content(req.getContent())
                .sortOrder(nextOrder)
                .build();
        return ArtistDto.CareerResponse.from(artistCareerRepository.save(career));
    }

    @Transactional
    public ArtistDto.CareerResponse updateCareer(String artistCode, Long careerId,
                                                  ArtistDto.CareerUpdateRequest req) {
        Artist artist = getArtistEntityByCode(artistCode);
        ArtistCareer career = artistCareerRepository.findById(careerId)
                .filter(c -> c.getArtist().getId().equals(artist.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        career.update(req.getCategory(), req.getYear(), req.getContent(),
                req.getSortOrder() != null ? req.getSortOrder() : career.getSortOrder());
        return ArtistDto.CareerResponse.from(career);
    }

    @Transactional
    public void deleteCareer(String artistCode, Long careerId) {
        Artist artist = getArtistEntityByCode(artistCode);
        ArtistCareer career = artistCareerRepository.findById(careerId)
                .filter(c -> c.getArtist().getId().equals(artist.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        artistCareerRepository.delete(career);
    }

    // ── 공통 ──────────────────────────────────────────────

    public Artist getArtistEntityByCode(String artistCode) {
        return artistRepository.findByArtistCode(artistCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}