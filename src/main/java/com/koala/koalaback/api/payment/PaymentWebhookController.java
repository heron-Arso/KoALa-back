package com.koala.koalaback.api.payment;

import com.koala.koalaback.domain.payment.service.PaymentService;
import com.koala.koalaback.global.response.ApiResponse;
import com.koala.koalaback.global.security.TossWebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 결제 PG사 웹훅 수신 컨트롤러
 * <p>
 * - POST /webhook/payments/toss : Toss Payments (Basic Auth 서명 검증)
 * - SecurityConfig 에서 /webhook/** permitAll 처리됨
 *
 * <p>KakaoPay 결제는 Toss easyPay를 통해 처리되므로 별도 KakaoPay 웹훅 엔드포인트가 없습니다.
 * KakaoPay 직접 연동이 필요할 경우 IP 화이트리스트 + 시그니처 검증을 반드시 구현하세요.
 */
@Slf4j
@RestController
@RequestMapping("/webhook/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final TossWebhookVerifier tossWebhookVerifier;

    // ── Toss Payments 웹훅 ────────────────────────────────────────────────────
    // Toss는 Authorization 헤더에 Basic base64(secretKey + ":") 형태로 서명 전달
    @PostMapping("/toss")
    public ResponseEntity<Void> tossWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody String payload) {

        if (!tossWebhookVerifier.verify(authorization)) {
            log.warn("[Webhook/Payments/Toss] 서명 검증 실패 — 요청 거부");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            paymentService.handleWebhook("TOSS", payload);
            log.info("[Webhook/Payments/Toss] 처리 완료");
        } catch (Exception e) {
            // Toss는 200 이외 응답 시 최대 5회 재시도 → 처리 실패 시 500 반환
            log.error("[Webhook/Payments/Toss] 처리 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok().build();
    }
}