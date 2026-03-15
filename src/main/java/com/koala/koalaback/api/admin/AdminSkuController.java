package com.koala.koalaback.api.admin;

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

@RestController
@RequestMapping("/admin/api/v1/skus")
@RequiredArgsConstructor
public class AdminSkuController {

    private final SkuService skuService;

    @GetMapping
    public ApiResponse<PageResponse<SkuDto.SummaryResponse>> getSkus(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(skuService.getAllSkus(pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkuDto.SummaryResponse> createSku(
            @Valid @RequestBody SkuDto.CreateRequest req) {
        return ApiResponse.ok(skuService.createSku(req));
    }

    @PutMapping("/{skuCode}")
    public ApiResponse<SkuDto.SummaryResponse> updateSku(
            @PathVariable String skuCode,
            @Valid @RequestBody SkuDto.UpdateRequest req) {
        return ApiResponse.ok(skuService.updateSku(skuCode, req));
    }

    @PatchMapping("/{skuCode}/publish")
    public ApiResponse<Void> publishSku(@PathVariable String skuCode) {
        skuService.publishSku(skuCode);
        return ApiResponse.ok();
    }

    @PatchMapping("/{skuCode}/discontinue")
    public ApiResponse<Void> discontinueSku(@PathVariable String skuCode) {
        skuService.discontinueSku(skuCode);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{skuCode}")
    public ApiResponse<Void> deleteSku(@PathVariable String skuCode) {
        skuService.deleteSku(skuCode);
        return ApiResponse.ok();
    }

    @PostMapping("/{skuCode}/360-frames")
    public ApiResponse<SkuDto.FrameListResponse> upload360Frames(
            @PathVariable String skuCode,
            @Valid @RequestBody java.util.List<SkuDto.FrameUploadItem> items) {
        return ApiResponse.ok(skuService.upload360Frames(skuCode, items));
    }

    @GetMapping("/{skuCode}/stock")
    public ApiResponse<SkuDto.StockResponse> getStock(@PathVariable String skuCode) {
        return ApiResponse.ok(skuService.getStock(skuCode));
    }
}