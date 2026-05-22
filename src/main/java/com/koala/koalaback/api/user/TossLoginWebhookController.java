package com.koala.koalaback.api.user;

import com.koala.koalaback.domain.user.entity.User;
import com.koala.koalaback.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 앱인토스 토스 로그인 연결 해제 웹훅
 *
 * 토스 앱 설정에서 유저가 연결 끊기를 하면 이 엔드포인트로 콜백이 옵니다.
 * POST /webhook/toss/login-disconnect
 *
 * TODO: 앱인토스 콘솔에서 웹훅 URL을 등록해야 합니다.
 * TODO: 요청 서명(Signature) 검증 로직 추가 필요
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook/toss")
public class TossLoginWebhookController {

    private final UserRepository userRepository;

    @PostMapping("/login-disconnect")
    public ResponseEntity<Void> handleDisconnect(@RequestBody Map<String, Object> payload) {
        try {
            String userKey = (String) payload.get("userKey");
            String referrer = (String) payload.get("referrer");  // USER_ACTION | TERMS_WITHDRAWAL | ACCOUNT_DELETION

            log.info("[TossWebhook] 연결 해제 수신 userKey={} referrer={}", userKey, referrer);

            if (userKey != null) {
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
