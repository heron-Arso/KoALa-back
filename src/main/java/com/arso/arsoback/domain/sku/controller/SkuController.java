package com.arso.arsoback.domain.sku.controller;

import com.arso.arsoback.domain.sku.dto.SkuCreateRequest;
import com.arso.arsoback.domain.sku.dto.SkuResponse;
import com.arso.arsoback.domain.sku.dto.SkuUpdateRequest;
import com.arso.arsoback.domain.sku.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skus")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @PostMapping
    public SkuResponse create(@RequestBody SkuCreateRequest request) {
        return skuService.create(request);
    }

    @GetMapping("/{id}")
    public SkuResponse get(@PathVariable Long id) {
        return skuService.get(id);
    }

    @GetMapping
    public List<SkuResponse> getAll() {
        return skuService.getAll();
    }

    @PutMapping("/{id}")
    public SkuResponse update(@PathVariable Long id,
                              @RequestBody SkuUpdateRequest request) {
        return skuService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        skuService.delete(id);
    }
}