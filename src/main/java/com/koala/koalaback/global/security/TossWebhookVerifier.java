package com.koala.koalaback.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Toss Payments Webhook 서명 검증
 * <p>
 * Toss는 웹훅 요청 시 Authorization 헤더에 아래 형식의 값을 보냄:
 * {@code Basic base64(webhookSecretKey + ":")}
 * 저장된 시크릿으로 동일한 토큰을 만들어 비교 검증.
 */
@Component
public class TossWebhookVerifier {

    private final String expectedToken;

    public TossWebhookVerifier(
            @Value("${toss.webhook-secret:}") String webhookSecret) {

        if (webhookSecret == null || webhookSecret.isBlank()) {
            // 시크릿 미설정(로컬 개발) → 검증 스킵
            this.expectedToken = null;
        } else {
            // base64(secret + ":") — Toss Basic Auth 형식
            String raw = webhookSecret + ":";
            this.expectedToken = "Basic " +
                    Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * @param authorizationHeader 요청의 Authorization 헤더 값
     * @return 검증 통과 여부 (시크릿 미설정 시 항상 true)
     */
    public boolean verify(String authorizationHeader) {
        if (expectedToken == null) {
            return true; // 로컬 개발 환경
        }
        return expectedToken.equals(authorizationHeader);
    }
}
