package com.arso.arsoback.domain.order.dto;

import com.arso.arsoback.domain.order.entity.OrderSkuItem;

import java.math.BigDecimal;

public record OrderSkuItemResponse(
        Long id,
        Long orderId,
        Long skuId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public static OrderSkuItemResponse from(OrderSkuItem item) {
        return new OrderSkuItemResponse(
                item.getId(),
                item.getOrderId(),
                item.getSkuId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}