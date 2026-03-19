package com.koala.koalaback.global.security.oauth2;

public interface OAuth2UserInfo {
    String getOauthId();
    String getEmail();
    String getName();
    String getProvider();
}
