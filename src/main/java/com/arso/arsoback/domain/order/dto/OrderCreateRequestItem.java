package com.arso.arsoback.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderCreateRequestItem(
        @NotNull(message = "skuId는 필수입니다.")
        Long skuId,

        @Min(value = 1, message = "quantity는 1 이상이어야 합니다.")
        int quantity
) {}