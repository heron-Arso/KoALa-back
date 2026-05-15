package com.koala.koalaback.domain.user.dto;

import com.koala.koalaback.domain.user.entity.User;
import com.koala.koalaback.domain.user.entity.UserAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

        /** 8~64자, 영문자 + 숫자/특수문자 각 1자 이상 포함 */
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*[\\d\\W]).{8,64}$",
            message = "비밀번호는 8자 이상이며 영문자와 숫자/특수문자를 각각 1자 이상 포함해야 합니다."
        )
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

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*[\\d\\W]).{8,64}$",
            message = "비밀번호는 8자 이상이며 영문자와 숫자/특수문자를 각각 1자 이상 포함해야 합니다."
        )
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

        /** null 이면 현재 값 유지, true 이면 기본 배송지로 설정 */
        private Boolean isDefault;
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