package com.koala.koalaback.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * IP 기반 Rate Limit 필터 (Redis 기반, 멀티 인스턴스 안전)
 *
 * [Tier 1] 민감 엔드포인트: IP당 5회 / 1분
 *   - /api/v1/auth/login, /api/v1/auth/signup
 *   - /api/v1/auth/password-reset/send
 *   - /admin/api/v1/auth/login  ← 어드민 브루트포스 방지
 *
 * [Tier 2] 전체 API: IP당 200회 / 1분
 *   - /api/**, /admin/api/**
 *
 * Redis 장애 시 fail-open (허용) — 서비스 가용성 우선
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    // Tier 1 — 민감 엔드포인트
    private static final int    SENSITIVE_LIMIT      = 5;
    private static final int    SENSITIVE_WINDOW_SEC = 60;

    // Tier 2 — 전체 API
    private static final int    GLOBAL_LIMIT         = 200;
    private static final int    GLOBAL_WINDOW_SEC    = 60;

    private static final Set<String> SENSITIVE_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/v1/auth/password-reset/send",
            "/admin/api/v1/auth/login"
    );

    /**
     * Lua 스크립트: INCR + 최초 키 생성 시 TTL 설정 (원자적 실행)
     * Redis는 싱글스레드 + Lua 스크립트 원자성 보장 → 레이스 컨디션 없음
     */
    private static final DefaultRedisScript<Long> INCR_SCRIPT = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1])\n" +
            "if tonumber(c) == 1 then\n" +
            "  redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))\n" +
            "end\n" +
            "return c",
            Long.class
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip   = resolveClientIp(request);

        // Tier 1: 민감 엔드포인트 체크
        if (SENSITIVE_PATHS.contains(path)) {
            if (!isAllowed(ip, "sensitive:" + path, SENSITIVE_LIMIT, SENSITIVE_WINDOW_SEC)) {
                log.warn("[RateLimit] 민감 경로 초과 — ip={}, path={}", ip, path);
                writeRateLimitResponse(response);
                return;
            }
        }

        // Tier 2: 전체 API 체크
        if (path.startsWith("/api/") || path.startsWith("/admin/api/")) {
            if (!isAllowed(ip, "global", GLOBAL_LIMIT, GLOBAL_WINDOW_SEC)) {
                log.warn("[RateLimit] 전체 API 초과 — ip={}", ip);
                writeRateLimitResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Redis INCR 기반 카운터 체크.
     * @return true = 허용, false = 차단
     */
    private boolean isAllowed(String ip, String scope, int limit, int windowSec) {
        String key = "rl:" + scope + ":" + ip;
        try {
            Long count = redisTemplate.execute(
                    INCR_SCRIPT,
                    List.of(key),
                    String.valueOf(windowSec)
            );
            return count == null || count <= limit;
        } catch (Exception e) {
            // Redis 장애 → fail-open (서비스 가용성 우선)
            log.warn("[RateLimit] Redis 오류, 요청 허용 처리: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 클라이언트 실제 IP 해석.
     * 로드밸런서(ALB/Nginx) 뒤: X-Forwarded-For 첫 번째 항목이 원본 IP
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"code\":\"C008\"," +
                "\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}"
        );
    }
}
