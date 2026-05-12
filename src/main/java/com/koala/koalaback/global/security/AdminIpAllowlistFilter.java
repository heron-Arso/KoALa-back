package com.koala.koalaback.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin API IP 화이트리스트 필터
 * <p>
 * /admin/api/** 경로에 대해 허용된 IP에서만 접근을 허용.
 * admin.allowed-ips 가 비어있으면 모든 IP 허용 (로컬 개발 편의).
 * <p>
 * 운영 환경에서는 ADMIN_ALLOWED_IPS 환경변수에 콤마 구분 IP 목록을 설정:
 * 예) ADMIN_ALLOWED_IPS=203.0.113.10,203.0.113.11
 */
@Slf4j
public class AdminIpAllowlistFilter extends OncePerRequestFilter {

    private static final String ADMIN_PREFIX = "/admin/api/";
    // 로그인은 IP 제한에서 제외 (IP가 바뀌어도 최초 로그인 가능)
    private static final String ADMIN_LOGIN_PATH = "/admin/api/v1/auth/login";

    private final Set<String> allowedIps;

    public AdminIpAllowlistFilter(String allowedIpsConfig) {

        if (allowedIpsConfig == null || allowedIpsConfig.isBlank()) {
            this.allowedIps = Set.of(); // 비어있으면 모두 허용
        } else {
            this.allowedIps = Arrays.stream(allowedIpsConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Admin API 경로가 아니면 통과
        if (!uri.startsWith(ADMIN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 로그인 엔드포인트는 IP 제한 제외
        if (uri.equals(ADMIN_LOGIN_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        // IP 목록 미설정(로컬 개발) → 통과
        if (allowedIps.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);

        if (!allowedIps.contains(clientIp)) {
            log.warn("Admin 접근 거부 — IP: {}, URI: {}", clientIp, uri);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"허용되지 않은 IP 주소입니다.\"}}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 리버스 프록시 환경 고려 IP 추출
     * 우선순위: X-Forwarded-For → X-Real-IP → RemoteAddr
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
