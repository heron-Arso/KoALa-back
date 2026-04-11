package com.koala.koalaback.global.config;

import com.koala.koalaback.global.security.JwtFilter;
import com.koala.koalaback.global.security.JwtProvider;
import com.koala.koalaback.global.security.oauth2.CustomOAuth2UserService;
import com.koala.koalaback.global.security.oauth2.OAuth2FailureHandler;
import com.koala.koalaback.global.security.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final Environment environment;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── 보안 헤더 ──────────────────────────────────────────
                .headers(headers -> headers
                        // HSTS: HTTPS 강제 (1년, 서브도메인 포함)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true)
                        )
                        // CSP: XSS 방어
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                        "script-src 'self'; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data: https:; " +
                                        "font-src 'self' data:; " +
                                        "connect-src 'self'; " +
                                        "frame-ancestors 'none'; " +
                                        "upgrade-insecure-requests"
                                )
                        )
                        // Clickjacking 방어
                        .frameOptions(frame -> frame.deny())
                        // MIME 스니핑 방지
                        .contentTypeOptions(contentType -> {})
                )

                .authorizeHttpRequests(auth -> {
                        auth.requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/signup",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/password-reset/send",
                                "/api/v1/auth/password-reset/verify",
                                "/api/v1/auth/password-reset/reset"
                        ).permitAll();
                        auth.requestMatchers(HttpMethod.GET,
                                "/api/v1/artists/**",
                                "/api/v1/skus/**",
                                "/api/v1/banners/**").permitAll();
                        auth.requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**").permitAll();
                        auth.requestMatchers("/webhook/**").permitAll();

                        // Swagger: 프로덕션에서는 ADMIN 전용, 개발환경에서는 공개
                        if (isProd) {
                            auth.requestMatchers("/swagger-ui/**", "/api-docs/**").hasRole("ADMIN");
                        } else {
                            auth.requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll();
                        }

                        auth.requestMatchers("/actuator/health").permitAll();
                        auth.requestMatchers("/admin/api/**").hasRole("ADMIN");
                        auth.anyRequest().authenticated();
                })
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .addFilterBefore(new JwtFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "capacitor://localhost",
                "http://localhost"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Cache-Control"
        ));
        // HttpOnly 쿠키는 JS에서 직접 접근 불가 — Set-Cookie 노출 불필요
        // config.setExposedHeaders(List.of("Set-Cookie"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}