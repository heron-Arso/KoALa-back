package com.koala.koalaback.domain.payment.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentProvider implements PaymentProvider {

    private static final String TOSS_API_BASE = "https://api.tosspayments.com/v1/payments";

    /** 기본값 없음 — 미설정 시 애플리케이션 기동 실패 (운영 환경 미설정 방지) */
    @Value("${toss.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;  // Spring 관리 빈 주입 (JacksonConfig)
    private final Environment environment;

    /**
     * 기동 시 시크릿 키 유효성 검증.
     * 운영(prod) 프로필에서 테스트 키 사용 시 즉시 기동 실패.
     */
    @PostConstruct
    void validateSecretKey() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "[Toss] toss.secret-key 가 설정되지 않았습니다. 환경변수 TOSS_SECRET_KEY 를 확인하세요.");
        }
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProd && secretKey.startsWith("test_sk_")) {
            throw new IllegalStateException(
                    "[Toss] 운영 환경에 테스트 시크릿 키(test_sk_*)를 사용할 수 없습니다. " +
                    "TOSS_SECRET_KEY 환경변수에 실제 운영 키를 설정하세요.");
        }
        if (secretKey.startsWith("test_sk_")) {
            log.warn("[Toss] 테스트 시크릿 키 사용 중 — 운영 배포 전 실제 키로 교체 필요");
        }
    }

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
            String rawJson = toJson(res);
            return new PaymentConfirmResult(
                    true,
                    (String) res.get("paymentKey"),
                    (String) res.get("approvalNo"),
                    new BigDecimal(res.get("totalAmount").toString()),
                    rawJson,
                    null, null
            );
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            String code = extractJsonField(body, "code");
            String msg  = extractJsonField(body, "message");
            log.error("Toss confirm rejected: orderId={}, code={}, message={}", orderId, code, msg);
            return new PaymentConfirmResult(false, null, null, null, null, code, msg);
        } catch (Exception e) {
            log.error("Toss confirm error: orderId={}, error={}", orderId, e.getMessage());
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
            return new PaymentCancelResult(true, cancelAmount, toJson(res), null, null);
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            String code = extractJsonField(body, "code");
            String msg  = extractJsonField(body, "message");
            log.error("Toss cancel rejected: pgTransactionId={}, code={}, message={}", pgTransactionId, code, msg);
            return new PaymentCancelResult(false, null, null, code, msg);
        } catch (Exception e) {
            log.error("Toss cancel error: pgTransactionId={}, error={}", pgTransactionId, e.getMessage());
            return new PaymentCancelResult(
                    false, null, null, "TOSS_CANCEL_ERROR", e.getMessage());
        }
    }

    /** Map → 유효한 JSON 문자열 변환 */
    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize response to JSON", e);
            return "{}";
        }
    }

    /** Toss 에러 응답 JSON에서 특정 필드 값 추출 */
    private String extractJsonField(String json, String field) {
        if (json == null || json.isBlank()) return null;
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
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