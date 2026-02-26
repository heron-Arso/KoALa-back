package com.arso.arsoback.domain.sku.controller;

import com.arso.arsoback.domain.sku.dto.SkuCreateRequest;
import com.arso.arsoback.domain.sku.dto.SkuResponse;
import com.arso.arsoback.domain.sku.dto.SkuUpdateRequest;
import com.arso.arsoback.domain.sku.service.SkuService;
import com.arso.arsoback.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skus")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @PostMapping
    public ApiResponse<SkuResponse> create(@Valid @RequestBody SkuCreateRequest request) {
        return ApiResponse.ok(skuService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<SkuResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(skuService.get(id));
    }

    @GetMapping
    public ApiResponse<List<SkuResponse>> getAll() {
        return ApiResponse.ok(skuService.getAll());
    }

    @PutMapping("/{id}")
    public ApiResponse<SkuResponse> update(@PathVariable Long id,
                                           @Valid @RequestBody SkuUpdateRequest request) {
        return ApiResponse.ok(skuService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        skuService.delete(id);
        return ApiResponse.ok();
    }
}