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
import com.koala.koalaback.domain.payment.dto.PaymentDto;
import com.koala.koalaback.domain.payment.repository.PaymentRepository;
import com.koala.koalaback.domain.payment.service.PaymentService;
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
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
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

        // ① 환불 먼저 시도 — 실패 시 예외를 던져 트랜잭션 전체 롤백
        // (취소 성공 후 환불 실패로 사용자가 돈을 돌려받지 못하는 상황 방지)
        paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(order.getId())
                .filter(p -> "CAPTURED".equals(p.getStatus()))
                .ifPresent(p -> {
                    paymentService.cancel(p.getPaymentNo(),
                            new PaymentDto.CancelRequest("주문취소", null));
                    log.info("Payment refunded on order cancel: paymentNo={}", p.getPaymentNo());
                });

        // ② 환불 성공(또는 결제 없음) 후 재고 복구 및 주문 취소
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

    /**
     * 미결제 만료 주문 1건 자동 취소 + 재고 복구 (스케줄러 전용).
     * <p>각 주문을 독립 트랜잭션으로 처리한다. 조회 후 결제됐을 수 있으므로
     * 트랜잭션 안에서 상태를 재확인하고, PENDING_PAYMENT 가 아니면 건드리지 않는다.
     * 결제 전 주문이라 환불 대상(CAPTURED 결제)은 없다.
     */
    @Transactional
    public void expirePendingOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return;
        if (!"PENDING_PAYMENT".equals(order.getOrderStatus())) return; // 그 사이 결제/취소됨

        order.getOrderItems().forEach(item -> {
            if (item.getSku() != null) {
                stockService.restore(item.getSku().getId(), item.getQuantity(),
                        "order_expiry", item.getId());
            }
        });
        order.cancel();
        log.info("미결제 만료 주문 자동취소: orderNo={}", order.getOrderNo());
    }

    /** 관리자 강제 취소 — 모든 상태 취소 가능, 이유 필수, 부분환불 지원 */
    @Transactional
    public OrderDto.OrderDetailResponse adminCancelOrder(String orderNo, OrderDto.AdminCancelRequest req) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 이미 취소된 주문이면 예외
        if ("CANCELLED".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        // 재고 복구 (배송완료 상태라도 재고를 돌려줌)
        order.getOrderItems().forEach(item -> {
            if (item.getSku() != null) {
                stockService.restore(item.getSku().getId(), item.getQuantity(),
                        "admin_cancel", item.getId());
            }
        });

        order.forceCancel();
        log.info("Admin force cancel: orderNo={}, reason={}", orderNo, req.getReason());

        // CAPTURED 결제 자동 환불
        paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(order.getId())
                .filter(p -> "CAPTURED".equals(p.getStatus()))
                .ifPresent(p -> {
                    try {
                        paymentService.cancel(p.getPaymentNo(),
                                new PaymentDto.CancelRequest(req.getReason(), req.getCancelAmount()));
                        log.info("Admin refund success: paymentNo={}, amount={}",
                                p.getPaymentNo(), req.getCancelAmount());
                    } catch (Exception e) {
                        log.error("Admin refund FAILED — manual action required: paymentNo={}, orderNo={}, error={}",
                                p.getPaymentNo(), orderNo, e.getMessage());
                        paymentService.recordRefundFailure(p.getPaymentNo(),
                                "어드민 강제취소 환불 실패 — 수동처리 필요: " + e.getMessage());
                    }
                });

        return OrderDto.OrderDetailResponse.from(order);
    }

    /** 관리자 주문 상세 (트랜잭션 안에서 lazy 컬렉션 접근) */
    public OrderDto.OrderDetailResponse getAdminOrderDetail(String orderNo) {
        return OrderDto.OrderDetailResponse.from(getOrderEntityByNo(orderNo));
    }

    /** 관리자 전체 주문 목록 (트랜잭션 안에서 lazy 컬렉션 접근) */
    public PageResponse<OrderDto.OrderSummaryResponse> getAdminOrders(Pageable pageable) {
        return PageResponse.of(
                orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                        .map(OrderDto.OrderSummaryResponse::from)
        );
    }

    /** 관리자 주문 검색 — 회원ID / 주문자명 / 전화번호 */
    public PageResponse<OrderDto.OrderSummaryResponse> adminSearchOrders(
            Long userId, String name, String phone, Pageable pageable) {
        return PageResponse.of(
                orderRepository.searchOrders(
                        userId,
                        (name  != null && !name.isBlank())  ? name  : null,
                        (phone != null && !phone.isBlank()) ? phone : null,
                        pageable
                ).map(OrderDto.OrderSummaryResponse::from)
        );
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