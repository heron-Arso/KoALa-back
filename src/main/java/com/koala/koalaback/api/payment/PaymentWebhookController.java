package com.koala.koalaback.api.payment;

import com.koala.koalaback.domain.payment.service.PaymentService;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/webhook/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @Value("${toss.webhook-secret}")
    private String tossWebhookSecret;

    // ── Toss Payments 웹훅 ────────────────────────────────────
    // Toss는 요청 헤더에 HMAC-SHA256 서명(Base64)을 포함해 전달
    // Header: TossPayments-Signature
    @PostMapping("/toss")
    public ApiResponse<Void> tossWebhook(
            @RequestHeader(value = "TossPayments-Signature", required = false) String signature,
            @RequestBody String payload) {

        verifyTossSignature(signature, payload);
        paymentService.handleWebhook("TOSS", payload);
        return ApiResponse.ok();
    }

    @PostMapping("/kakaopay")
    public ApiResponse<Void> kakaoPayWebhook(@RequestBody String payload) {
        // KakaoPay 웹훅은 IP 화이트리스트 + TLS로 신뢰 (별도 서명 없음)
        paymentService.handleWebhook("KAKAOPAY", payload);
        return ApiResponse.ok();
    }

    // ── 서명 검증 헬퍼 ───────────────────────────────────────
    private void verifyTossSignature(String signature, String payload) {
        if (signature == null || signature.isBlank()) {
            log.warn("[Webhook] Toss 서명 헤더 누락 — 요청 거부");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing webhook signature");
        }

        // 더미 시크릿(로컬 개발)일 경우 서명 검증 건너뜀
        if (tossWebhookSecret.startsWith("dummy")) {
            log.warn("[Webhook] 더미 시크릿 사용 중 — 서명 검증 생략 (개발 환경 전용)");
            return;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    tossWebhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedBase64 = Base64.getEncoder().encodeToString(expected);

            if (!expectedBase64.equals(signature)) {
                log.warn("[Webhook] Toss 서명 불일치 — 요청 거부");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Webhook] 서명 검증 중 오류: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Signature verification failed");
        }
    }
}