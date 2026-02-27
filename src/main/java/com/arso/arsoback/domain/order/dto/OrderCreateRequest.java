package com.arso.arsoback.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderCreateRequest(
        @NotNull(message = "userId는 필수입니다.")
        Long userId,

        @NotEmpty(message = "skuItems는 최소 1개 이상이어야 합니다.")
        List<@Valid OrderCreateRequestItem> skuItems
) {}