package com.arso.arsoback.domain.order.service;

import com.arso.arsoback.domain.order.dto.OrderSkuItemResponse;
import com.arso.arsoback.domain.order.repository.OrderSkuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderSkuItemService {

    private final OrderSkuItemRepository orderSkuItemRepository;

    public List<OrderSkuItemResponse> getItems(Long orderId) {
        return orderSkuItemRepository.findAllByOrderId(orderId)
                .stream()
                .map(OrderSkuItemResponse::from)
                .toList();
    }
}