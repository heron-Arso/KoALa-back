package com.koala.koalaback.api.user;

import com.koala.koalaback.domain.user.dto.UserDto;
import com.koala.koalaback.domain.user.service.TossLoginService;
import com.koala.koalaback.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

/**
 * 앱인토스 토스 로그인 컨트롤러
 *
 * POST /api/v1/auth/toss/login
 *   - 앱인토스 SDK appLogin() 에서 받은 authorizationCode를 받아 JWT 쿠키 발급
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/toss")
public class TossLoginController {

    private final TossLoginService tossLoginService;

    @Value("${jwt.access-token-expiry-ms:1800000}")
    private long accessTokenExpiryMs;

    @Value("${jwt.refresh-token-expiry-ms:604800000}")
    private long refreshTokenExpiryMs;

    @Value("${app.secure-cookies:false}")
    private boolean secureCookies;

    @PostMapping("/login")
    public ApiResponse<Void> tossLogin(
            @Valid @RequestBody TossLoginRequest req,
            HttpServletResponse response) {

        UserDto.TokenResponse tokens = tossLoginService.login(req.authorizationCode());
        setTokenCookies(response, tokens);
        return ApiResponse.ok();
    }

    // ── Request DTO ─────────────────────────────────────────────────────────────

    public record TossLoginRequest(
            @NotBlank String authorizationCode,
            String referrer  // "DEFAULT" | "SANDBOX" — 로깅/분기용
    ) {}

    // ── Cookie helpers ───────────────────────────────────────────────────────────

    private void setTokenCookies(HttpServletResponse response, UserDto.TokenResponse tokens) {
        String sameSite = secureCookies ? "None" : "Lax";
        response.addHeader("Set-Cookie",
                ResponseCookie.from("accessToken", tokens.getAccessToken())
                        .httpOnly(true)
                        .secure(secureCookies)
                        .path("/")
                        .maxAge(accessTokenExpiryMs / 1000)
                        .sameSite(sameSite)
                        .build().toString());
        response.addHeader("Set-Cookie",
                ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                        .httpOnly(true)
                        .secure(secureCookies)
                        .path("/api/v1/auth")
                        .maxAge(refreshTokenExpiryMs / 1000)
                        .sameSite(sameSite)
                        .build().toString());
    }
}
