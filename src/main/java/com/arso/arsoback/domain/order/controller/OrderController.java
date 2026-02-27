package com.arso.arsoback.domain.order.controller;

import com.arso.arsoback.domain.order.dto.OrderCreateRequest;
import com.arso.arsoback.domain.order.dto.OrderResponse;
import com.arso.arsoback.domain.order.dto.OrderSkuItemResponse;
import com.arso.arsoback.domain.order.service.OrderService;
import com.arso.arsoback.domain.order.service.OrderSkuItemService;
import com.arso.arsoback.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderSkuItemService orderSkuItemService;

    @PostMapping
    public ApiResponse<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.ok(orderService.create(request));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> get(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.get(orderId));
    }

    // ✅ 프론트용: 주문 아이템 목록 조회
    @GetMapping("/{orderId}/items")
    public ApiResponse<List<OrderSkuItemResponse>> getItems(@PathVariable Long orderId) {
        // 주문 존재 검증 (없으면 ORDER_NOT_FOUND로 터짐)
        orderService.getEntity(orderId);

        return ApiResponse.ok(orderSkuItemService.getItems(orderId));
    }
}