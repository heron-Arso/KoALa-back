package com.arso.arsoback.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentConfirmRequest(
        @NotNull(message = "orderId는 필수입니다.")
        Long orderId,

        @NotBlank(message = "paymentKey는 필수입니다.")
        String paymentKey,

        @NotNull(message = "amount는 필수입니다.")
        @Positive(message = "amount는 0보다 커야 합니다.")
        BigDecimal amount
) {}