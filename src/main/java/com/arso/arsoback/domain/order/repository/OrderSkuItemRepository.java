package com.arso.arsoback.domain.order.repository;

import com.arso.arsoback.domain.order.entity.OrderSkuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderSkuItemRepository extends JpaRepository<OrderSkuItem, Long> {
    List<OrderSkuItem> findAllByOrderId(Long orderId);
}