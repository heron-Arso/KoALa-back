package com.koala.koalaback.global.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

/**
 * 로그인·회원가입·비밀번호 재설정 등 민감 엔드포인트에 대한 IP 기반 속도 제한.
 *
 * 운영 시 주의:
 *  - 단일 인스턴스 메모리 기반 구현 — 멀티 인스턴스 환경에서는 Redis(bucket4j-redis) 권장
 *  - 로드밸런서/프록시 뒤에서는 X-Forwarded-For 헤더의 첫 번째 값을 사용
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 5;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    /** bucket map 최대 크기 — 초과 시 LRU 방식으로 가장 오래된 항목 제거 */
    private static final int MAX_BUCKETS = 10_000;

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/v1/auth/password-reset/send"
    );

    /** access-order LRU 맵 — synchronizedMap으로 thread-safe 처리 */
    private final Map<String, Bucket> buckets = Collections.synchronizedMap(
            new LinkedHashMap<>(MAX_BUCKETS + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                    return size() > MAX_BUCKETS;
                }
            }
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!RATE_LIMITED_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);
        String bucketKey = ip + ":" + path;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded: ip={}, path={}", ip, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"code\":\"C008\",\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}"
            );
        }
    }

    /**
     * 클라이언트 실제 IP 해석.
     *
     * 로드밸런서(ALB/Nginx) 뒤에서는 X-Forwarded-For의 첫 번째 항목이 실제 클라이언트 IP.
     * X-Real-IP를 fallback으로 사용.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // "client, proxy1, proxy2" 형식 → 첫 번째가 원본 클라이언트
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(CAPACITY)
                        .refillIntervally(CAPACITY, REFILL_PERIOD)
                        .build())
                .build();
    }
}
