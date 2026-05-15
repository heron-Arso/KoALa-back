package com.koala.koalaback.global.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "token_blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProvider jwtProvider;

    /**
     * 토큰을 블랙리스트에 등록.
     * Redis 장애 시 로그만 남기고 로그아웃은 정상 처리 (클라이언트 토큰 삭제로 충분)
     */
    public void blacklist(String token) {
        long remainingMs = jwtProvider.getRemainingExpiryMs(token);
        if (remainingMs <= 0) return;
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + token,
                    "1",
                    Duration.ofMillis(remainingMs)
            );
        } catch (Exception e) {
            log.warn("[TokenBlacklist] Redis 저장 실패 — 로그아웃은 계속 진행됨: {}", e.getMessage());
        }
    }

    /**
     * 토큰 블랙리스트 여부 확인.
     * Redis 장애 시 true 반환 (fail-secure) — 로그아웃된 토큰의 재사용을 차단.
     * Redis 복구 전까지 해당 사용자는 재로그인이 필요합니다.
     */
    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
        } catch (Exception e) {
            log.warn("[TokenBlacklist] Redis 조회 실패 — 보안상 블랙리스트 처리로 간주 (fail-secure): {}", e.getMessage());
            return true; // fail-secure: Redis 장애 시 토큰 재사용 차단
        }
    }
}
