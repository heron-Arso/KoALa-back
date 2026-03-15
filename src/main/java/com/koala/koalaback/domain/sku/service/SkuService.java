package com.koala.koalaback.domain.sku.service;

import com.koala.koalaback.domain.artist.entity.Artist;
import com.koala.koalaback.domain.artist.service.ArtistService;
import com.koala.koalaback.domain.sku.dto.SkuDto;
import com.koala.koalaback.domain.sku.entity.Sku;
import com.koala.koalaback.domain.sku.entity.SkuMedia;
import com.koala.koalaback.domain.sku.entity.SkuReviewStats;
import com.koala.koalaback.domain.sku.repository.SkuMediaRepository;
import com.koala.koalaback.domain.sku.repository.SkuRepository;
import com.koala.koalaback.domain.sku.repository.SkuReviewStatsRepository;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.global.response.PageResponse;
import com.koala.koalaback.global.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkuService {

    private final SkuRepository skuRepository;
    private final SkuMediaRepository skuMediaRepository;
    private final SkuReviewStatsRepository skuReviewStatsRepository;
    private final ArtistService artistService;
    private final StockService stockService;
    private final CodeGenerator codeGenerator;

    // ── 공개 조회 ─────────────────────────────────────────

    public PageResponse<SkuDto.SummaryResponse> getActiveSkus(Pageable pageable) {
        Page<SkuDto.SummaryResponse> page = skuRepository
                .findByStatusAndDeletedAtIsNull("ACTIVE", pageable)
                .map(this::toSummary);
        return PageResponse.of(page);
    }

    public PageResponse<SkuDto.SummaryResponse> getSkusByArtist(String artistCode, Pageable pageable) {
        Artist artist = artistService.getArtistEntityByCode(artistCode);
        Page<SkuDto.SummaryResponse> page = skuRepository
                .findByArtistIdAndStatusAndDeletedAtIsNull(artist.getId(), "ACTIVE", pageable)
                .map(this::toSummary);
        return PageResponse.of(page);
    }

    public SkuDto.DetailResponse getSkuBySlug(String slug) {
        Sku sku = skuRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));
        return toDetail(sku);
    }

    public SkuDto.DetailResponse getSkuByCode(String skuCode) {
        return toDetail(getSkuEntityByCode(skuCode));
    }

    // ── 360도 프레임 조회 (Redis 캐시) ────────────────────

    @Cacheable(value = "sku360frames", key = "#skuCode")
    public SkuDto.FrameListResponse get360Frames(String skuCode) {
        Sku sku = getSkuEntityByCode(skuCode);
        List<SkuMedia> frames = skuMediaRepository
                .findBySkuIdAndMediaRoleOrderByAngleDegreeAsc(sku.getId(), "SPINE_360");
        return SkuDto.FrameListResponse.builder()
                .skuCode(skuCode)
                .frameCount(frames.size())
                .frames(frames.stream().map(SkuDto.MediaResponse::from).toList())
                .build();
    }

    // ── 어드민 CRUD ───────────────────────────────────────

    @Transactional
    public SkuDto.SummaryResponse createSku(SkuDto.CreateRequest req) {
        if (skuRepository.existsBySlug(req.getSlug())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
        }
        Artist artist = artistService.getArtistEntityByCode(req.getArtistCode());
        Sku sku = Sku.builder()
                .skuCode(codeGenerator.generateCode())
                .artist(artist)
                .name(req.getName())
                .slug(req.getSlug())
                .description(req.getDescription())
                .skuType(req.getSkuType())
                .genre(req.getGenre())
                .listPrice(req.getListPrice())
                .salePrice(req.getSalePrice())
                .isLimitedEdition(req.getIsLimitedEdition())
                .editionSize(req.getEditionSize())
                .editionNumber(req.getEditionNumber())
                .primaryImageUrl(req.getPrimaryImageUrl())
                .widthCm(req.getWidthCm())
                .heightCm(req.getHeightCm())
                .depthCm(req.getDepthCm())
                .weightKg(req.getWeightKg())
                .build();
        skuRepository.save(sku);
        skuReviewStatsRepository.save(SkuReviewStats.builder().sku(sku).build());
        return toSummary(sku);
    }

    @Transactional
    @CacheEvict(value = "sku360frames", key = "#skuCode")
    public SkuDto.SummaryResponse updateSku(String skuCode, SkuDto.UpdateRequest req) {
        Sku sku = getSkuEntityByCode(skuCode);
        if (!sku.getSlug().equals(req.getSlug()) && skuRepository.existsBySlug(req.getSlug())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
        }
        sku.update(req.getName(), req.getSlug(), req.getDescription(),
                req.getListPrice(), req.getSalePrice(), req.getPrimaryImageUrl());
        return toSummary(sku);
    }

    @Transactional
    public void publishSku(String skuCode) {
        getSkuEntityByCode(skuCode).publish();
    }

    @Transactional
    public void discontinueSku(String skuCode) {
        getSkuEntityByCode(skuCode).discontinue();
    }

    @Transactional
    public void deleteSku(String skuCode) {
        getSkuEntityByCode(skuCode).softDelete();
    }

    // ── 360도 프레임 업로드 (어드민) ──────────────────────

    @Transactional
    @CacheEvict(value = "sku360frames", key = "#skuCode")
    public SkuDto.FrameListResponse upload360Frames(String skuCode, List<SkuDto.FrameUploadItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 각도 범위 및 중복 검증
        items.forEach(i -> {
            if (i.getAngleDegree() == null
                    || i.getAngleDegree().doubleValue() < 0
                    || i.getAngleDegree().doubleValue() >= 360) {
                throw new BusinessException(ErrorCode.INVALID_ANGLE_DEGREE);
            }
        });

        long distinctCount = items.stream()
                .map(i -> i.getAngleDegree().stripTrailingZeros().toPlainString())
                .distinct()
                .count();
        if (distinctCount != items.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_ANGLE_DEGREE);
        }

        Sku sku = getSkuEntityByCode(skuCode);
        skuMediaRepository.deleteBySkuIdAndMediaRole(sku.getId(), "SPINE_360");

        List<SkuDto.FrameUploadItem> sorted = items.stream()
                .sorted(Comparator.comparing(SkuDto.FrameUploadItem::getAngleDegree))
                .toList();

        List<SkuMedia> mediaList = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            SkuDto.FrameUploadItem item = sorted.get(i);
            mediaList.add(SkuMedia.builder()
                    .sku(sku)
                    .mediaType("IMAGE")
                    .mediaRole("SPINE_360")
                    .fileUrl(item.getFileUrl())
                    .thumbnailUrl(item.getThumbnailUrl())
                    .angleDegree(item.getAngleDegree())
                    .sortOrder(i)
                    .isPrimary(false)
                    .build());
        }
        skuMediaRepository.saveAll(mediaList);

        return SkuDto.FrameListResponse.builder()
                .skuCode(skuCode)
                .frameCount(mediaList.size())
                .frames(mediaList.stream().map(SkuDto.MediaResponse::from).toList())
                .build();
    }

    // ── 재고 조회 ─────────────────────────────────────────

    public SkuDto.StockResponse getStock(String skuCode) {
        Sku sku = getSkuEntityByCode(skuCode);
        return SkuDto.StockResponse.builder()
                .skuCode(skuCode)
                .stockQuantity(stockService.getStock(sku.getId()))
                .build();
    }

    // ── Package-level helpers ─────────────────────────────

    public Sku getSkuEntityByCode(String skuCode) {
        return skuRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));
    }

    public Sku getSkuEntityById(Long id) {
        return skuRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));
    }

    // ── Private helpers ───────────────────────────────────

    private SkuDto.SummaryResponse toSummary(Sku sku) {
        int stock = stockService.getStock(sku.getId());
        SkuReviewStats stats = skuReviewStatsRepository.findById(sku.getId()).orElse(null);
        return SkuDto.SummaryResponse.from(sku, stock, stats);
    }

    private SkuDto.DetailResponse toDetail(Sku sku) {
        int stock = stockService.getStock(sku.getId());
        SkuReviewStats stats = skuReviewStatsRepository.findById(sku.getId()).orElse(null);
        List<SkuMedia> media = skuMediaRepository
                .findBySkuIdOrderByMediaRoleAscSortOrderAsc(sku.getId());
        return SkuDto.DetailResponse.from(sku, stock, stats, media);
    }
    public PageResponse<SkuDto.SummaryResponse> getAllSkus(Pageable pageable) {
        Page<SkuDto.SummaryResponse> page = skuRepository
                .findByDeletedAtIsNull(pageable)
                .map(this::toSummary);
        return PageResponse.of(page);
    }
}