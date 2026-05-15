package com.koala.koalaback.domain.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koala.koalaback.domain.order.entity.Order;
import com.koala.koalaback.domain.order.repository.OrderRepository;
import com.koala.koalaback.domain.payment.dto.PaymentDto;
import com.koala.koalaback.domain.payment.entity.Payment;
import com.koala.koalaback.domain.payment.entity.PaymentEvent;
import com.koala.koalaback.domain.payment.provider.PaymentProvider;
import com.koala.koalaback.domain.payment.repository.PaymentEventRepository;
import com.koala.koalaback.domain.payment.repository.PaymentRepository;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.global.util.CodeGenerator;
import com.koala.koalaback.infra.mail.EmailService;
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
    private final OrderRepository orderRepository;
    private final CodeGenerator codeGenerator;
    private final List<PaymentProvider> providers;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @Transactional
    public PaymentDto.PrepareResponse prepare(Long userId, PaymentDto.PrepareRequest req) {
        Order order = orderRepository.findByOrderNo(req.getOrderNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

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
        Order order = orderRepository.findByOrderNo(req.getOrderNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Payment payment = paymentRepository
                .findTopByOrderIdOrderByCreatedAtDesc(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 이미 처리된 결제(paymentKey 재사용) 방지
        if (!"READY".equals(payment.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

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

            // 주문 완료 이메일 — @Async 비동기 발송 (결제 응답 속도에 영향 없음)
            sendOrderConfirmEmailAsync(order);
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
        String paymentKey = extractTransactionId(payloadJson);
        if (paymentKey.isBlank()) {
            log.warn("Webhook paymentKey 추출 실패 — payload: {}", payloadJson);
            return;
        }
        paymentRepository.findByPgTransactionId(paymentKey)
                .ifPresentOrElse(
                        payment -> {
                            String status = extractWebhookStatus(payloadJson);
                            recordEvent(payment, "WEBHOOK", status, BigDecimal.ZERO, paymentKey, payloadJson);
                            log.info("Webhook processed: paymentKey={}, status={}", paymentKey, status);
                        },
                        () -> log.warn("Webhook — 매핑된 결제 없음: paymentKey={}", paymentKey)
                );
    }

    /**
     * 환불 실패 이벤트 기록 — 주문 취소/반품 승인 후 환불이 실패했을 때 감사 추적용
     */
    @Transactional
    public void recordRefundFailure(String paymentNo, String reason) {
        paymentRepository.findByPaymentNo(paymentNo).ifPresent(payment ->
                recordEvent(payment, "REFUND_FAILED", "FAILED", BigDecimal.ZERO, null, reason)
        );
    }

    private void sendOrderConfirmEmailAsync(Order order) {
        try {
            List<EmailService.OrderConfirmData.ItemData> items = order.getOrderItems().stream()
                    .map(i -> new EmailService.OrderConfirmData.ItemData(
                            i.getSkuNameSnapshot(),
                            i.getQuantity(),
                            i.getLineTotalAmount()))
                    .toList();

            emailService.sendOrderConfirmEmail(new EmailService.OrderConfirmData(
                    order.getOrdererEmail(),
                    order.getOrdererName(),
                    order.getOrderNo(),
                    items,
                    order.getProductAmount(),
                    order.getShippingAmount(),
                    order.getTotalAmount()
            ));
        } catch (Exception e) {
            // 이메일 실패가 결제 트랜잭션에 영향을 주어서는 안 됨 — 로그만 남기고 계속
            log.warn("주문 완료 이메일 발송 준비 실패 (결제는 정상): orderNo={}, error={}",
                    order.getOrderNo(), e.getMessage());
        }
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

    /**
     * Toss Payments 웹훅 payload에서 paymentKey 추출
     * Toss 웹훅 형식: { "eventType": "...", "data": { "paymentKey": "...", ... } }
     */
    private String extractTransactionId(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return "";
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            // Toss 표준 웹훅 형식: data.paymentKey
            JsonNode dataNode = root.path("data");
            if (!dataNode.isMissingNode()) {
                String key = dataNode.path("paymentKey").asText("");
                if (!key.isBlank()) return key;
            }
            // 최상위 paymentKey fallback (일부 이벤트 타입)
            return root.path("paymentKey").asText("");
        } catch (Exception e) {
            log.warn("Webhook payload 파싱 실패: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 웹훅 이벤트 상태 추출
     * Toss 웹훅: data.status (DONE, CANCELED, PARTIAL_CANCELED, etc.)
     */
    private String extractWebhookStatus(String payloadJson) {
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            String status = root.path("data").path("status").asText("");
            return status.isBlank() ? "UNKNOWN" : status;
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}