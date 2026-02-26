package com.arso.arsoback.domain.sku.dto;

import com.arso.arsoback.domain.sku.entity.Sku;

import java.math.BigDecimal;

public record SkuResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stock,
        String status
) {
    public static SkuResponse from(Sku sku) {
        return new SkuResponse(
                sku.getId(),
                sku.getName(),
                sku.getDescription(),
                sku.getPrice(),
                sku.getStock(),
                sku.getStatus()
        );
    }
}