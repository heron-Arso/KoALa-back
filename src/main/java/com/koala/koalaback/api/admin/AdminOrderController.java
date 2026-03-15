package com.koala.koalaback.api.admin;

import com.koala.koalaback.domain.order.dto.OrderDto;
import com.koala.koalaback.domain.order.entity.Order;
import com.koala.koalaback.domain.order.repository.OrderRepository;
import com.koala.koalaback.domain.order.service.OrderService;
import com.koala.koalaback.global.response.ApiResponse;
import com.koala.koalaback.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/v1/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @GetMapping
    public ApiResponse<PageResponse<OrderDto.OrderSummaryResponse>> getOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.of(
                orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                        .map(OrderDto.OrderSummaryResponse::from)
        ));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderDto.OrderDetailResponse> getOrder(
            @PathVariable String orderNo) {
        return ApiResponse.ok(OrderDto.OrderDetailResponse.from(
                orderService.getOrderEntityByNo(orderNo)
        ));
    }

    @PatchMapping("/{orderNo}/tracking")
    public ApiResponse<Void> registerTracking(
            @PathVariable String orderNo,
            @Valid @RequestBody OrderDto.RegisterTrackingRequest req) {
        orderService.registerTracking(orderNo, req);
        return ApiResponse.ok();
    }

    @PatchMapping("/{orderNo}/delivered")
    public ApiResponse<Void> markDelivered(@PathVariable String orderNo) {
        orderService.markDelivered(orderNo);
        return ApiResponse.ok();
    }
}