package com.koala.koalaback.api.payment;

import com.koala.koalaback.domain.payment.service.PaymentService;
import com.koala.koalaback.global.security.TossWebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Toss Payments 웹훅 수신 엔드포인트
 * <p>
 * - URL: POST /webhook/toss
 * - SecurityConfig에서 /webhook/** permitAll 처리됨
 * - Authorization 헤더로 서명 검증 후 PaymentService 에 위임
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TossWebhookController {

    private final PaymentService paymentService;
    private final TossWebhookVerifier webhookVerifier;

    @PostMapping("/webhook/toss")
    public ResponseEntity<Void> handleTossWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody String payload) {

        // ── 1. 서명 검증 ───────────────────────────────────────────
        if (!webhookVerifier.verify(authorization)) {
            log.warn("Toss 웹훅 서명 검증 실패 — 요청 무시");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // ── 2. 이벤트 처리 ─────────────────────────────────────────
        try {
            paymentService.handleWebhook("TOSS", payload);
            log.info("Toss 웹훅 처리 완료");
        } catch (Exception e) {
            // Toss는 200 이외 응답 시 최대 5회 재시도
            // 처리 실패 시 500 반환 → Toss 재시도 유도
            log.error("Toss 웹훅 처리 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok().build();
    }
}
