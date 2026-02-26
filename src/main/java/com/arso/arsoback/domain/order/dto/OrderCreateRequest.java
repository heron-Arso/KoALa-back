package com.arso.arsoback.domain.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderCreateRequest(
        @NotNull(message = "userId는 필수입니다.")
        Long userId,

        @NotNull(message = "totalAmount는 필수입니다.")
        @Positive(message = "totalAmount는 0보다 커야 합니다.")
        BigDecimal totalAmount
) {}