package com.koala.koalaback.api.user;

import com.koala.koalaback.domain.user.entity.User;
import com.koala.koalaback.domain.user.repository.UserRepository;
import com.koala.koalaback.global.security.TossWebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 앱인토스 토스 로그인 연결 해제 웹훅
 *
 * 토스 앱 설정에서 유저가 연결 끊기를 하면 이 엔드포인트로 콜백이 옵니다.
 * POST /webhook/toss/login-disconnect
 *
 * TODO: 앱인토스 콘솔에서 웹훅 URL을 등록해야 합니다.
 * NOTE: 서명 검증은 Authorization 헤더(Basic base64(secret + ":")) 기준이며,
 *       앱인토스 로그인 웹훅의 실제 인증 방식이 다르면 TossWebhookVerifier 를 조정해야 합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook/toss")
public class TossLoginWebhookController {

    private final UserRepository userRepository;
    private final TossWebhookVerifier webhookVerifier;

    @PostMapping("/login-disconnect")
    @Transactional
    public ResponseEntity<Void> handleDisconnect(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload) {

        // 서명(인증) 검증 — 미인증 요청으로 임의 유저 연결 해제되는 것을 차단
        if (!webhookVerifier.verify(authHeader)) {
            log.warn("[TossWebhook] 서명 검증 실패 — 요청 거부");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String userKey = (String) payload.get("userKey");
            String referrer = (String) payload.get("referrer");  // USER_ACTION | TERMS_WITHDRAWAL | ACCOUNT_DELETION

            log.info("[TossWebhook] 연결 해제 수신 userKey={} referrer={}", userKey, referrer);

            if (userKey != null) {
                // @Transactional 영속성 컨텍스트 내에서 변경 → dirty checking 으로 저장됨
                userRepository.findByOauthProviderAndOauthId("TOSS", userKey)
                        .ifPresent(User::disconnectToss);
            }
        } catch (Exception e) {
            log.error("[TossWebhook] 처리 중 오류 발생: {}", e.getMessage(), e);
        }

        // 토스 서버는 200 응답을 기대함
        return ResponseEntity.ok().build();
    }
}
