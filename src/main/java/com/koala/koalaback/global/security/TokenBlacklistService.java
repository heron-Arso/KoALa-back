package com.koala.koalaback.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "token_blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProvider jwtProvider;

    public void blacklist(String token) {
        long remainingMs = jwtProvider.getRemainingExpiryMs(token);
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + token,
                    "1",
                    Duration.ofMillis(remainingMs)
            );
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
    }
}
