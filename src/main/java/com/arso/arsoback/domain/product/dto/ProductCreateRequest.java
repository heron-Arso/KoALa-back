package com.arso.arsoback.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80) String artistName,
        @NotNull @Min(0) Integer price,
        @NotNull Boolean limited,
        @Size(max = 500) String thumbnailUrl
) {}