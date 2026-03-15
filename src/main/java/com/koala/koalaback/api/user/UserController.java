package com.koala.koalaback.api.user;

import com.koala.koalaback.domain.user.dto.UserDto;
import com.koala.koalaback.domain.user.service.UserService;
import com.koala.koalaback.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── Auth ──────────────────────────────────────────────

    @PostMapping("/api/v1/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDto.TokenResponse> signup(
            @Valid @RequestBody UserDto.SignupRequest req) {
        return ApiResponse.ok(userService.signup(req));
    }

    @PostMapping("/api/v1/auth/login")
    public ApiResponse<UserDto.TokenResponse> login(
            @Valid @RequestBody UserDto.LoginRequest req) {
        return ApiResponse.ok(userService.login(req));
    }

    @PostMapping("/api/v1/auth/refresh")
    public ApiResponse<UserDto.TokenResponse> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        return ApiResponse.ok(userService.refresh(refreshToken));
    }

    @PostMapping("/api/v1/auth/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Long userId) {
        userService.logout(userId);
        return ApiResponse.ok();
    }

    // ── Profile ───────────────────────────────────────────

    @GetMapping("/api/v1/users/me")
    public ApiResponse<UserDto.ProfileResponse> getProfile(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PatchMapping("/api/v1/users/me")
    public ApiResponse<UserDto.ProfileResponse> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserDto.UpdateProfileRequest req) {
        return ApiResponse.ok(userService.updateProfile(userId, req));
    }

    @PatchMapping("/api/v1/users/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserDto.ChangePasswordRequest req) {
        userService.changePassword(userId, req);
        return ApiResponse.ok();
    }

    @DeleteMapping("/api/v1/users/me")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ApiResponse.ok();
    }

    // ── Address ───────────────────────────────────────────

    @GetMapping("/api/v1/users/me/addresses")
    public ApiResponse<List<UserDto.AddressResponse>> getAddresses(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getAddresses(userId));
    }

    @PostMapping("/api/v1/users/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDto.AddressResponse> createAddress(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserDto.AddressCreateRequest req) {
        return ApiResponse.ok(userService.createAddress(userId, req));
    }

    @PutMapping("/api/v1/users/me/addresses/{addressId}")
    public ApiResponse<UserDto.AddressResponse> updateAddress(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody UserDto.AddressUpdateRequest req) {
        return ApiResponse.ok(userService.updateAddress(userId, addressId, req));
    }

    @PatchMapping("/api/v1/users/me/addresses/{addressId}/default")
    public ApiResponse<Void> setDefaultAddress(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long addressId) {
        userService.setDefaultAddress(userId, addressId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/api/v1/users/me/addresses/{addressId}")
    public ApiResponse<Void> deleteAddress(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long addressId) {
        userService.deleteAddress(userId, addressId);
        return ApiResponse.ok();
    }
}