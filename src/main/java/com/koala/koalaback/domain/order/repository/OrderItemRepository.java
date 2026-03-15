package com.koala.koalaback.domain.order.repository;

import com.koala.koalaback.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    // ReviewService에서 사용 — 본인 주문의 아이템인지 검증
    Optional<OrderItem> findByIdAndOrderUserId(Long id, Long userId);
}