package com.arso.arsoback.domain.order.service;

import com.arso.arsoback.domain.order.dto.OrderCreateRequest;
import com.arso.arsoback.domain.order.dto.OrderResponse;
import com.arso.arsoback.domain.order.entity.Order;
import com.arso.arsoback.domain.order.entity.OrderSkuItem;
import com.arso.arsoback.domain.order.repository.OrderRepository;
import com.arso.arsoback.domain.order.repository.OrderSkuItemRepository;
import com.arso.arsoback.domain.sku.entity.Sku;
import com.arso.arsoback.domain.sku.repository.SkuRepository;
import com.arso.arsoback.global.exception.BusinessException;
import com.arso.arsoback.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderSkuItemRepository orderSkuItemRepository;
    private final SkuRepository skuRepository;

    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        if (request.skuItems() == null || request.skuItems().isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_ITEMS);
        }

        // 1) SKU 가격 합산
        BigDecimal total = BigDecimal.ZERO;

        // 주문 먼저 생성(아이템이 orderId 필요)
        Order order = new Order(request.userId(), BigDecimal.ZERO);
        Order savedOrder = orderRepository.save(order);

        for (var item : request.skuItems()) {
            Sku sku = skuRepository.findById(item.skuId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));

            // 재고 검증(지금은 단순 검증만)
            if (sku.getStock() < item.quantity()) {
                throw new BusinessException(ErrorCode.SKU_OUT_OF_STOCK, "SKU 재고가 부족합니다. skuId=" + item.skuId());
            }

            BigDecimal unitPrice = sku.getPrice();
            OrderSkuItem line = new OrderSkuItem(savedOrder.getId(), sku.getId(), item.quantity(), unitPrice);
            orderSkuItemRepository.save(line);

            total = total.add(line.getLineTotal());
        }

        // 2) 주문 총액 확정
        savedOrder.updateTotalAmount(total);

        return OrderResponse.from(savedOrder);
    }

    public Order getEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    public OrderResponse get(Long orderId) {
        return OrderResponse.from(getEntity(orderId));
    }
}