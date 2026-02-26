package com.arso.arsoback.domain.payment.service;

import com.arso.arsoback.domain.order.entity.Order;
import com.arso.arsoback.domain.order.entity.OrderStatus;
import com.arso.arsoback.domain.order.service.OrderService;
import com.arso.arsoback.domain.payment.dto.PaymentConfirmRequest;
import com.arso.arsoback.domain.payment.dto.PaymentConfirmResponse;
import com.arso.arsoback.domain.payment.entity.Payment;
import com.arso.arsoback.domain.payment.entity.PaymentStatus;
import com.arso.arsoback.domain.payment.gateway.PaymentGatewayClient;
import com.arso.arsoback.domain.payment.repository.PaymentRepository;
import com.arso.arsoback.global.exception.BusinessException;
import com.arso.arsoback.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final PaymentGatewayClient paymentGatewayClient;

    @Transactional
    public PaymentConfirmResponse confirm(PaymentConfirmRequest request) {
        Order order = orderService.getEntity(request.orderId());

        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }

        // 1) (현재는 더미) 결제사 검증
        PaymentGatewayClient.VerifiedPayment verified = paymentGatewayClient.verify(request.paymentKey());
        if (!verified.success()) {
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }

        // 2) 금액 검증: (요청 amount == 주문 totalAmount) AND (검증 amount == 주문 totalAmount)
        BigDecimal orderAmount = order.getTotalAmount();

        if (request.amount() == null || orderAmount.compareTo(request.amount()) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        if (verified.amount() == null || orderAmount.compareTo(verified.amount()) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 3) 결제 저장 + 주문 PAID 처리
        Payment payment = new Payment(order.getId(), request.paymentKey(), orderAmount, PaymentStatus.VERIFIED);
        Payment saved = paymentRepository.save(payment);

        order.markPaid(); // 주문 상태 변경 (영속 상태면 flush 시 반영)

        return PaymentConfirmResponse.from(saved);
    }
}