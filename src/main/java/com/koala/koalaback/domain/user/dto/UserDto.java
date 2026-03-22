package com.koala.koalaback.domain.user.dto;

import com.koala.koalaback.domain.user.entity.User;
import com.koala.koalaback.domain.user.entity.UserAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class UserDto {

    // ── Requests ──────────────────────────────────────────

    @Getter
    public static class SignupRequest {
        @NotBlank @Email
        private String email;

        @NotBlank @Size(min = 8, max = 64)
        private String password;

        @NotBlank @Size(max = 100)
        private String name;

        private String phone;
    }

    @Getter
    public static class LoginRequest {
        @NotBlank @Email
        private String email;

        @NotBlank
        private String password;
    }

    @Getter
    public static class UpdateProfileRequest {
        @Size(max = 100)
        private String name;

        @Size(max = 30)
        private String phone;
    }

    @Getter
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank @Size(min = 8, max = 64)
        private String newPassword;
    }

    @Getter
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;
    }

    // ── Responses ─────────────────────────────────────────

    @Getter
    @Builder
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;

        public static TokenResponse of(String accessToken, String refreshToken) {
            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ProfileResponse {
        private Long id;
        private String userCode;
        private String email;
        private String name;
        private String phone;
        private String status;
        private String oauthProvider;
        private LocalDateTime createdAt;

        public static ProfileResponse from(User user) {
            return ProfileResponse.builder()
                    .id(user.getId())
                    .userCode(user.getUserCode())
                    .email(user.getEmail())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .status(user.getStatus())
                    .oauthProvider(user.getOauthProvider())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

    // ── Address Requests ──────────────────────────────────

    @Getter
    public static class AddressCreateRequest {
        private String label;

        @NotBlank
        private String recipientName;

        @NotBlank
        private String recipientPhone;

        @NotBlank
        private String zipCode;

        @NotBlank
        private String address1;

        private String address2;
        private Boolean isDefault;
    }

    @Getter
    public static class AddressUpdateRequest {
        private String label;

        @NotBlank
        private String recipientName;

        @NotBlank
        private String recipientPhone;

        @NotBlank
        private String zipCode;

        @NotBlank
        private String address1;

        private String address2;
    }

    // ── Address Response ──────────────────────────────────

    @Getter
    @Builder
    public static class AddressResponse {
        private Long id;
        private String label;
        private String recipientName;
        private String recipientPhone;
        private String zipCode;
        private String address1;
        private String address2;
        private Boolean isDefault;

        public static AddressResponse from(UserAddress address) {
            return AddressResponse.builder()
                    .id(address.getId())
                    .label(address.getLabel())
                    .recipientName(address.getRecipientName())
                    .recipientPhone(address.getRecipientPhone())
                    .zipCode(address.getZipCode())
                    .address1(address.getAddress1())
                    .address2(address.getAddress2())
                    .isDefault(address.getIsDefault())
                    .build();
        }
    }
}