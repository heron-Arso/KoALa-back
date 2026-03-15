package com.koala.koalaback.api.order;

import com.koala.koalaback.domain.order.dto.OrderDto;
import com.koala.koalaback.domain.order.service.OrderService;
import com.koala.koalaback.global.response.ApiResponse;
import com.koala.koalaback.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/api/v1/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderDto.OrderDetailResponse> createOrder(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OrderDto.CreateRequest req) {
        return ApiResponse.ok(orderService.createOrder(userId, req));
    }

    @GetMapping("/api/v1/orders")
    public ApiResponse<PageResponse<OrderDto.OrderSummaryResponse>> getMyOrders(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(orderService.getMyOrders(userId, pageable));
    }

    @GetMapping("/api/v1/orders/{orderNo}")
    public ApiResponse<OrderDto.OrderDetailResponse> getMyOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderNo) {
        return ApiResponse.ok(orderService.getMyOrder(userId, orderNo));
    }

    @PostMapping("/api/v1/orders/{orderNo}/cancel")
    public ApiResponse<OrderDto.OrderDetailResponse> cancelOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderNo) {
        return ApiResponse.ok(orderService.cancelOrder(userId, orderNo));
    }

    @PatchMapping("/admin/api/v1/orders/{orderNo}/tracking")
    public ApiResponse<Void> registerTracking(
            @PathVariable String orderNo,
            @Valid @RequestBody OrderDto.RegisterTrackingRequest req) {
        orderService.registerTracking(orderNo, req);
        return ApiResponse.ok();
    }

    @PatchMapping("/admin/api/v1/orders/{orderNo}/delivered")
    public ApiResponse<Void> markDelivered(@PathVariable String orderNo) {
        orderService.markDelivered(orderNo);
        return ApiResponse.ok();
    }
}