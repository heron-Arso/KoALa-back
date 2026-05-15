package com.koala.koalaback.global.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

/**
 * 클라이언트 실제 IP 추출 유틸리티 (스푸핑 방지)
 *
 * <p>X-Forwarded-For / X-Real-IP 헤더는 요청이 신뢰된 사설/로컬 대역에서
 * 들어온 경우에만 신뢰합니다. 외부 클라이언트가 헤더를 직접 주입해도
 * RemoteAddr 가 공인 IP 이면 헤더를 무시하고 RemoteAddr 를 반환합니다.
 *
 * <p>사용처: {@link com.koala.koalaback.global.security.RateLimitFilter},
 * {@link com.koala.koalaback.domain.admin.service.AdminService}
 */
public final class IpResolverUtil {

    // 신뢰된 리버스 프록시 대역 (사설망 + 루프백)
    private static final Set<String> TRUSTED_PREFIXES = Set.of(
            "127.", "10.",
            "172.16.", "172.17.", "172.18.", "172.19.", "172.20.",
            "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
            "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31.",
            "192.168."
    );
    private static final Set<String> TRUSTED_EXACT = Set.of("::1", "0:0:0:0:0:0:0:1");

    private IpResolverUtil() {}

    /**
     * 요청에서 클라이언트 IP를 추출합니다.
     * RemoteAddr 가 신뢰된 프록시 대역일 때만 X-Forwarded-For / X-Real-IP 를 신뢰합니다.
     */
    public static String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (isTrustedProxy(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                return xRealIp.trim();
            }
        }

        return remoteAddr;
    }

    private static boolean isTrustedProxy(String addr) {
        if (addr == null) return false;
        if (TRUSTED_EXACT.contains(addr)) return true;
        return TRUSTED_PREFIXES.stream().anyMatch(addr::startsWith);
    }
}
