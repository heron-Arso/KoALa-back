package com.koala.koalaback.domain.order.service;

import com.koala.koalaback.domain.cart.entity.Cart;
import com.koala.koalaback.domain.cart.entity.CartItem;
import com.koala.koalaback.domain.cart.service.CartService;
import com.koala.koalaback.domain.order.dto.OrderDto;
import com.koala.koalaback.domain.order.entity.Order;
import com.koala.koalaback.domain.order.entity.OrderItem;
import com.koala.koalaback.domain.order.entity.OrderShipment;
import com.koala.koalaback.domain.order.repository.OrderItemRepository;
import com.koala.koalaback.domain.order.repository.OrderRepository;
import com.koala.koalaback.domain.order.repository.OrderShipmentRepository;
import com.koala.koalaback.domain.sku.entity.Sku;
import com.koala.koalaback.domain.sku.service.StockService;
import com.koala.koalaback.domain.user.service.UserService;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.global.response.PageResponse;
import com.koala.koalaback.global.util.CodeGenerator;
import com.koala.koalaback.global.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderShipmentRepository orderShipmentRepository;
    private final CartService cartService;
    private final StockService stockService;
    private final UserService userService;
    private final CodeGenerator codeGenerator;
    private final PhoneNormalizer phoneNormalizer;

    @Transactional
    public OrderDto.OrderDetailResponse createOrder(Long userId, OrderDto.CreateRequest req) {
        Cart cart = cartService.getOrCreateCart(userId);

        List<CartItem> selectedItems = selectCartItems(cart, req.getCartItemIds());
        if (selectedItems.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 재고 검증 및 차감
        for (CartItem ci : selectedItems) {
            Sku sku = ci.getSku();
            if (!sku.isAvailable()) throw new BusinessException(ErrorCode.SKU_NOT_ACTIVE);
            stockService.deduct(sku.getId(), ci.getQuantity(), "order_items", null);
        }

        // 금액 계산
        BigDecimal productAmount = selectedItems.stream()
                .map(CartItem::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingAmount = productAmount.compareTo(new BigDecimal("50000")) >= 0
                ? BigDecimal.ZERO : new BigDecimal("3000");
        BigDecimal totalAmount = productAmount.add(shippingAmount);

        String phone = phoneNormalizer.normalize(req.getOrdererPhone());

        Order order = Order.builder()
                .orderNo(codeGenerator.generateOrderNo())
                .user(userService.getUserById(userId))
                .productAmount(productAmount)
                .discountAmount(BigDecimal.ZERO)
                .shippingAmount(shippingAmount)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .ordererName(req.getOrdererName())
                .ordererEmail(req.getOrdererEmail())
                .ordererPhone(phone)
                .build();
        orderRepository.save(order);

        // 주문 아이템 저장
        List<OrderItem> orderItems = selectedItems.stream().map(ci -> {
            Sku sku = ci.getSku();
            return OrderItem.builder()
                    .order(order)
                    .sku(sku)
                    .artist(sku.getArtist())
                    .skuCodeSnapshot(sku.getSkuCode())
                    .artistCodeSnapshot(sku.getArtist().getArtistCode())
                    .skuNameSnapshot(sku.getName())
                    .artistNameSnapshot(sku.getArtist().getName())
                    .quantity(ci.getQuantity())
                    .unitPrice(ci.getUnitPrice())
                    .lineTotalAmount(ci.getLineAmount())
                    .build();
        }).toList();
        orderItemRepository.saveAll(orderItems);
        order.getOrderItems().addAll(orderItems);

        // 배송지 저장
        OrderDto.ShipmentRequest sr = req.getShipment();
        String shipPhone = phoneNormalizer.normalize(sr.getRecipientPhone());
        OrderShipment shipment = OrderShipment.builder()
                .order(order)
                .recipientName(sr.getRecipientName())
                .recipientPhone(shipPhone)
                .zipCode(sr.getZipCode())
                .address1(sr.getAddress1())
                .address2(sr.getAddress2())
                .deliveryRequest(sr.getDeliveryRequest())
                .build();
        orderShipmentRepository.save(shipment);

        // 장바구니에서 주문 완료 아이템 제거
        cart.getItems().removeAll(selectedItems);

        log.info("Order created: orderNo={}, userId={}, total={}",
                order.getOrderNo(), userId, totalAmount);
        return OrderDto.OrderDetailResponse.from(order);
    }

    public PageResponse<OrderDto.OrderSummaryResponse> getMyOrders(Long userId, Pageable pageable) {
        return PageResponse.of(
                orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                        .map(OrderDto.OrderSummaryResponse::from)
        );
    }

    public OrderDto.OrderDetailResponse getMyOrder(Long userId, String orderNo) {
        Order order = orderRepository.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return OrderDto.OrderDetailResponse.from(order);
    }

    @Transactional
    public OrderDto.OrderDetailResponse cancelOrder(Long userId, String orderNo) {
        Order order = orderRepository.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.isCancellable()) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        order.getOrderItems().forEach(item -> {
            if (item.getSku() != null) {
                stockService.restore(item.getSku().getId(), item.getQuantity(),
                        "order_items", item.getId());
            }
        });

        order.cancel();
        log.info("Order cancelled: orderNo={}, userId={}", orderNo, userId);
        return OrderDto.OrderDetailResponse.from(order);
    }

    @Transactional
    public void registerTracking(String orderNo, OrderDto.RegisterTrackingRequest req) {
        Order order = getOrderEntityByNo(orderNo);
        OrderShipment shipment = orderShipmentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        shipment.registerTracking(req.getCarrierCode(), req.getTrackingNo());
        order.markShipped();
    }

    @Transactional
    public void markDelivered(String orderNo) {
        Order order = getOrderEntityByNo(orderNo);
        order.markDelivered();
        OrderShipment shipment = orderShipmentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        shipment.markDelivered();
    }

    public Order getOrderEntityByNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private List<CartItem> selectCartItems(Cart cart, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return cart.getItems();
        return cart.getItems().stream()
                .filter(ci -> itemIds.contains(ci.getId()))
                .toList();
    }
}