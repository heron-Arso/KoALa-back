package com.koala.koalaback.domain.payment.service;

import com.koala.koalaback.domain.order.entity.Order;
import com.koala.koalaback.domain.order.service.OrderService;
import com.koala.koalaback.domain.payment.dto.PaymentDto;
import com.koala.koalaback.domain.payment.entity.Payment;
import com.koala.koalaback.domain.payment.entity.PaymentEvent;
import com.koala.koalaback.domain.payment.provider.PaymentProvider;
import com.koala.koalaback.domain.payment.repository.PaymentEventRepository;
import com.koala.koalaback.domain.payment.repository.PaymentRepository;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.global.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OrderService orderService;
    private final CodeGenerator codeGenerator;
    private final List<PaymentProvider> providers;

    @Transactional
    public PaymentDto.PrepareResponse prepare(Long userId, PaymentDto.PrepareRequest req) {
        Order order = orderService.getOrderEntityByNo(req.getOrderNo());

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!"PENDING_PAYMENT".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentNo(codeGenerator.generatePaymentNo())
                .provider(req.getProvider())
                .method(req.getMethod())
                .requestedAmount(order.getTotalAmount())
                .build();
        paymentRepository.save(payment);

        recordEvent(payment, "READY", "SUCCESS", order.getTotalAmount(), null, null);

        return PaymentDto.PrepareResponse.builder()
                .paymentNo(payment.getPaymentNo())
                .orderNo(order.getOrderNo())
                .amount(order.getTotalAmount())
                .provider(req.getProvider())
                .method(req.getMethod())
                .build();
    }

    @Transactional
    public PaymentDto.PaymentResponse confirm(Long userId, PaymentDto.ConfirmRequest req) {
        Order order = orderService.getOrderEntityByNo(req.getOrderNo());

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Payment payment = paymentRepository
                .findTopByOrderIdOrderByCreatedAtDesc(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getRequestedAmount().compareTo(req.getAmount()) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        PaymentProvider provider = getProvider(payment.getProvider());
        PaymentProvider.PaymentConfirmResult result =
                provider.confirm(req.getPaymentKey(), req.getOrderNo(), req.getAmount());

        if (result.success()) {
            payment.markCaptured(result.pgTransactionId(), result.approvalNo(),
                    result.approvedAmount(), result.rawResponse());
            order.markPaid();
            recordEvent(payment, "CAPTURED", "SUCCESS",
                    result.approvedAmount(), result.pgTransactionId(), result.rawResponse());
        } else {
            payment.markFailed(result.failureCode(), result.failureMessage());
            order.markPaymentFailed();
            recordEvent(payment, "FAILED", "FAILED",
                    req.getAmount(), null, result.failureMessage());
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                    result.failureMessage());
        }

        return PaymentDto.PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentDto.PaymentResponse cancel(String paymentNo, PaymentDto.CancelRequest req) {
        Payment payment = paymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.isCaptured()) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        BigDecimal cancelAmount = req.getCancelAmount() != null
                ? req.getCancelAmount() : payment.getApprovedAmount();

        PaymentProvider provider = getProvider(payment.getProvider());
        PaymentProvider.PaymentCancelResult result =
                provider.cancel(payment.getPgTransactionId(), cancelAmount, req.getReason());

        if (result.success()) {
            payment.markCancelled(cancelAmount);
            recordEvent(payment, "CANCELLED", "SUCCESS",
                    cancelAmount, null, result.rawResponse());
        } else {
            recordEvent(payment, "CANCELLED", "FAILED",
                    cancelAmount, null, result.failureMessage());
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                    result.failureMessage());
        }

        return PaymentDto.PaymentResponse.from(payment);
    }

    @Transactional
    public void handleWebhook(String providerCode, String payloadJson) {
        log.info("Webhook received: provider={}", providerCode);
        paymentRepository.findByPgTransactionId(extractTransactionId(payloadJson))
                .ifPresent(payment -> recordEvent(
                        payment, "WEBHOOK", "SUCCESS", BigDecimal.ZERO, null, payloadJson));
    }

    private PaymentProvider getProvider(String providerCode) {
        return providers.stream()
                .filter(p -> p.getProviderCode().equals(providerCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                        "지원하지 않는 PG사: " + providerCode));
    }

    private void recordEvent(Payment payment, String eventType, String eventStatus,
                             BigDecimal amount, String providerEventId, String payloadJson) {
        paymentEventRepository.save(PaymentEvent.builder()
                .payment(payment)
                .eventType(eventType)
                .eventStatus(eventStatus)
                .amount(amount)
                .providerEventId(providerEventId)
                .payloadJson(payloadJson)
                .build());
    }

    private String extractTransactionId(String payloadJson) {
        // 실제 구현 시 PG사별 JSON 파싱
        return "";
    }
}