package com.koala.koalaback.global.security.oauth2;

import java.util.Map;

public class NaverOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object>attributes;

    @SuppressWarnings("unchecked")
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getOauthId(){
        return (String) attributes.get("id");
    }
    @Override
    public String getEmail(){
        return (String) attributes.get("email");
    }
    @Override
    public String getName(){
        String name = (String) attributes.get("name");
        return name!= null?name : "네이버 사용자";
    }
    @Override
    public String getProvider(){return "NAVER";}
}
