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
 * admin.allowed-ips 가 비어있으면 localhost(127.0.0.1, ::1)만 허용.
 * <p>
 * 운영 환경에서는 ADMIN_ALLOWED_IPS 환경변수에 콤마 구분 IP 목록을 설정:
 * 예) ADMIN_ALLOWED_IPS=203.0.113.10,203.0.113.11
 */
@Slf4j
public class AdminIpAllowlistFilter extends OncePerRequestFilter {

    private static final String ADMIN_PREFIX = "/admin/api/";
    // 로그인은 IP 제한에서 제외 (IP가 바뀌어도 최초 로그인 가능)
    private static final String ADMIN_LOGIN_PATH = "/admin/api/v1/auth/login";

    // 리버스 프록시로 알려진 사설/로컬 대역 — 이 IP에서 온 요청만 X-Forwarded-For를 신뢰
    private static final Set<String> TRUSTED_PROXY_PREFIXES = Set.of(
            "127.", "10.", "172.16.", "172.17.", "172.18.", "172.19.", "172.20.",
            "172.21.", "172.22.", "172.23.", "172.24.", "172.25.", "172.26.", "172.27.",
            "172.28.", "172.29.", "172.30.", "172.31.", "192.168.", "0:0:0:0:0:0:0:1", "::1"
    );

    private final Set<String> allowedIps;

    public AdminIpAllowlistFilter(String allowedIpsConfig) {
        if (allowedIpsConfig == null || allowedIpsConfig.isBlank()) {
            // 환경변수 미설정 시 localhost만 허용 (전체 허용 방지)
            this.allowedIps = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1");
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
     * 스푸핑 방지 IP 추출 전략:
     * - X-Forwarded-For / X-Real-IP는 신뢰된 사설 대역에서 온 요청일 때만 신뢰
     *   (로컬/사설망 리버스 프록시에서만 이 헤더를 세팅)
     * - 외부 클라이언트가 직접 X-Forwarded-For를 주입해도 RemoteAddr가 공인 IP면 무시
     */
    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // 신뢰된 프록시에서 온 요청인지 확인
        boolean fromTrustedProxy = TRUSTED_PROXY_PREFIXES.stream()
                .anyMatch(remoteAddr::startsWith)
                || "::1".equals(remoteAddr)
                || "0:0:0:0:0:0:0:1".equals(remoteAddr);

        if (fromTrustedProxy) {
            // X-Forwarded-For의 첫 번째 항목 사용 (Nginx가 클라이언트 IP를 넣는 위치)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                return xRealIp.trim();
            }
        }

        // 신뢰되지 않은 요청 or 프록시 헤더 없음 → RemoteAddr 직접 사용
        return remoteAddr;
    }
}
