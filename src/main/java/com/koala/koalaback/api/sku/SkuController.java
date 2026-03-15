package com.koala.koalaback.api.sku;

import com.koala.koalaback.domain.sku.dto.SkuDto;
import com.koala.koalaback.domain.sku.service.SkuService;
import com.koala.koalaback.global.response.ApiResponse;
import com.koala.koalaback.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    // ── Public ────────────────────────────────────────────

    @GetMapping("/api/v1/skus")
    public ApiResponse<PageResponse<SkuDto.SummaryResponse>> getSkus(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(skuService.getActiveSkus(pageable));
    }

    @GetMapping("/api/v1/skus/{slug}")
    public ApiResponse<SkuDto.DetailResponse> getSku(@PathVariable String slug) {
        return ApiResponse.ok(skuService.getSkuBySlug(slug));
    }

    @GetMapping("/api/v1/skus/{skuCode}/360-frames")
    public ApiResponse<SkuDto.FrameListResponse> get360Frames(@PathVariable String skuCode) {
        return ApiResponse.ok(skuService.get360Frames(skuCode));
    }

    @GetMapping("/api/v1/artists/{artistCode}/skus")
    public ApiResponse<PageResponse<SkuDto.SummaryResponse>> getSkusByArtist(
            @PathVariable String artistCode,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(skuService.getSkusByArtist(artistCode, pageable));
    }

    // ── Admin ─────────────────────────────────────────────

    @PostMapping("/admin/api/v1/skus")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkuDto.SummaryResponse> createSku(
            @Valid @RequestBody SkuDto.CreateRequest req) {
        return ApiResponse.ok(skuService.createSku(req));
    }

    @PutMapping("/admin/api/v1/skus/{skuCode}")
    public ApiResponse<SkuDto.SummaryResponse> updateSku(
            @PathVariable String skuCode,
            @Valid @RequestBody SkuDto.UpdateRequest req) {
        return ApiResponse.ok(skuService.updateSku(skuCode, req));
    }

    @PatchMapping("/admin/api/v1/skus/{skuCode}/publish")
    public ApiResponse<Void> publishSku(@PathVariable String skuCode) {
        skuService.publishSku(skuCode);
        return ApiResponse.ok();
    }

    @PatchMapping("/admin/api/v1/skus/{skuCode}/discontinue")
    public ApiResponse<Void> discontinueSku(@PathVariable String skuCode) {
        skuService.discontinueSku(skuCode);
        return ApiResponse.ok();
    }

    @DeleteMapping("/admin/api/v1/skus/{skuCode}")
    public ApiResponse<Void> deleteSku(@PathVariable String skuCode) {
        skuService.deleteSku(skuCode);
        return ApiResponse.ok();
    }

    /** 360도 프레임 업로드 — S3 업로드는 별도 presigned URL로 처리 후 URL만 전달 */
    @PostMapping("/admin/api/v1/skus/{skuCode}/360-frames")
    public ApiResponse<SkuDto.FrameListResponse> upload360Frames(
            @PathVariable String skuCode,
            @Valid @RequestBody List< SkuDto.FrameUploadItem> items) {
        return ApiResponse.ok(skuService.upload360Frames(skuCode, items));
    }

    @GetMapping("/admin/api/v1/skus/{skuCode}/stock")
    public ApiResponse<SkuDto.StockResponse> getStock(@PathVariable String skuCode) {
        return ApiResponse.ok(skuService.getStock(skuCode));
    }
}