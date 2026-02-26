package com.arso.arsoback.domain.order.controller;

import com.arso.arsoback.domain.order.dto.OrderCreateRequest;
import com.arso.arsoback.domain.order.dto.OrderResponse;
import com.arso.arsoback.domain.order.service.OrderService;
import com.arso.arsoback.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.ok(orderService.create(request));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> get(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.get(orderId));
    }
}