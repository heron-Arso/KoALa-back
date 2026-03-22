package com.koala.koalaback.global.security.oauth2;

import com.koala.koalaback.domain.user.entity.RefreshToken;
import com.koala.koalaback.domain.user.entity.User;
import com.koala.koalaback.domain.user.repository.RefreshTokenRepository;
import com.koala.koalaback.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @Value("${oauth2.redirect-uri:http://localhost:3000/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        String accessToken  = jwtProvider.createAccessToken(user.getId(), "USER");
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // 리프레시 토큰 Redis 저장
        long expirySeconds = refreshTokenExpiryMs / 1000;
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .userId(String.valueOf(user.getId()))  // ← Long → String
                        .refreshToken(refreshToken)
                        .expiry(expirySeconds)
                        .build()
        );

        // 프론트엔드로 토큰 전달
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        log.info("OAuth2 login success: userId={}, redirect={}", user.getId(), redirectUri);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}