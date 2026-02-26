package com.arso.arsoback.domain.product.dto;

import java.time.OffsetDateTime;

public record ProductResponse(
        Long id,
        String name,
        String artistName,
        Integer price,
        Boolean limited,
        String thumbnailUrl,
        OffsetDateTime createdAt
) {}