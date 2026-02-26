package com.arso.arsoback.domain.order.repository;

import com.arso.arsoback.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}