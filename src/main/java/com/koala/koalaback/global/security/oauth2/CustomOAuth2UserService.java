package com.koala.koalaback.global.security.oauth2;

import com.koala.koalaback.domain.user.entity.User;
import com.koala.koalaback.domain.user.repository.UserRepository;
import com.koala.koalaback.global.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final CodeGenerator codeGenerator;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration()
                .getRegistrationId().toUpperCase();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory
                .getOAuth2UserInfo(provider, oAuth2User.getAttributes());

        // 1) 소셜 ID로 기존 회원 조회
        // 2) 없으면 이메일로 조회 → 일반 회원가입 계정에 OAuth 연결
        // 3) 둘 다 없으면 신규 가입
        User user = userRepository
                .findByOauthProviderAndOauthId(provider, userInfo.getOauthId())
                .orElseGet(() -> userRepository.findByEmail(userInfo.getEmail())
                        .map(existing -> {
                            existing.linkOAuth(provider, userInfo.getOauthId());
                            log.info("OAuth2 linked to existing account: email={}, provider={}", userInfo.getEmail(), provider);
                            return existing;
                        })
                        .orElseGet(() -> registerNewOAuthUser(userInfo)));

        // 이름 최신화
        user.updateOAuthInfo(userInfo.getName());

        log.info("OAuth2 login: provider={}, userId={}", provider, user.getId());

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private User registerNewOAuthUser(OAuth2UserInfo userInfo) {
        log.info("New OAuth2 user: provider={}, email={}",
                userInfo.getProvider(), userInfo.getEmail());

        return userRepository.save(
                User.createOAuthUser(
                        codeGenerator.generateCode(),
                        userInfo.getEmail(),
                        userInfo.getName(),
                        userInfo.getProvider(),
                        userInfo.getOauthId()
                )
        );
    }
}