package com.koala.koalaback.domain.payment.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentProvider implements PaymentProvider {

    private static final String TOSS_API_BASE = "https://api.tosspayments.com/v1/payments";

    @Value("${toss.secret-key:test_sk_placeholder}")
    private String secretKey;

    private final RestTemplate restTemplate;

    @Override
    public String getProviderCode() { return "TOSS"; }

    @Override
    public PaymentConfirmResult confirm(String paymentKey, String orderId, BigDecimal amount) {
        try {
            HttpHeaders headers = buildHeaders();
            // Toss API: KRW는 소수점 없는 정수(Long) 필수 — BigDecimal 그대로 전송 시 오류
            Map<String, Object> body = Map.of(
                    "paymentKey", paymentKey,
                    "orderId", orderId,
                    "amount", amount.longValue()
            );
            ResponseEntity<Map> response = restTemplate.exchange(
                    TOSS_API_BASE + "/confirm",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            Map<String, Object> res = response.getBody();
            return new PaymentConfirmResult(
                    true,
                    (String) res.get("paymentKey"),
                    (String) res.get("approvalNo"),
                    new BigDecimal(res.get("totalAmount").toString()),
                    res.toString(),
                    null, null
            );
        } catch (Exception e) {
            log.error("Toss confirm failed: orderId={}, error={}", orderId, e.getMessage());
            return new PaymentConfirmResult(
                    false, null, null, null, null, "TOSS_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentCancelResult cancel(String pgTransactionId,
                                      BigDecimal cancelAmount, String reason) {
        try {
            HttpHeaders headers = buildHeaders();
            Map<String, Object> body = Map.of(
                    "cancelReason", reason,
                    "cancelAmount", cancelAmount
            );
            ResponseEntity<Map> response = restTemplate.exchange(
                    TOSS_API_BASE + "/" + pgTransactionId + "/cancel",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            Map<String, Object> res = response.getBody();
            return new PaymentCancelResult(true, cancelAmount, res.toString(), null, null);
        } catch (Exception e) {
            log.error("Toss cancel failed: pgTransactionId={}, error={}", pgTransactionId, e.getMessage());
            return new PaymentCancelResult(
                    false, null, null, "TOSS_CANCEL_ERROR", e.getMessage());
        }
    }

    private HttpHeaders buildHeaders() {
        String encoded = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encoded);
        return headers;
    }
}