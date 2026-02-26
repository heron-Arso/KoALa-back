package com.arso.arsoback.domain.order.dto;

import com.arso.arsoback.domain.order.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long userId,
        BigDecimal totalAmount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getId(),
                o.getUserId(),
                o.getTotalAmount(),
                o.getStatus().name(),
                o.getCreatedAt(),
                o.getPaidAt()
        );
    }
}