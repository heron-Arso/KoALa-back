package com.arso.arsoback.domain.order.service;

import com.arso.arsoback.domain.order.dto.OrderCreateRequest;
import com.arso.arsoback.domain.order.dto.OrderResponse;
import com.arso.arsoback.domain.order.entity.Order;
import com.arso.arsoback.domain.order.repository.OrderRepository;
import com.arso.arsoback.global.exception.BusinessException;
import com.arso.arsoback.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        Order order = new Order(request.userId(), request.totalAmount());
        return OrderResponse.from(orderRepository.save(order));
    }

    public Order getEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    public OrderResponse get(Long orderId) {
        return OrderResponse.from(getEntity(orderId));
    }
}