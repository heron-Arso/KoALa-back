package com.arso.arsoback.domain.payment.dto;

import com.arso.arsoback.domain.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentConfirmResponse(
        Long paymentId,
        Long orderId,
        String paymentKey,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) {
    public static PaymentConfirmResponse from(Payment p) {
        return new PaymentConfirmResponse(
                p.getId(),
                p.getOrderId(),
                p.getPaymentKey(),
                p.getAmount(),
                p.getStatus().name(),
                p.getCreatedAt()
        );
    }
}