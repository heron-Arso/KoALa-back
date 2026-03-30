package com.koala.koalaback.api.user;

import com.koala.koalaback.domain.user.dto.PasswordResetDto;
import com.koala.koalaback.domain.user.service.PasswordResetService;
import com.koala.koalaback.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;
    @PostMapping("/password-reset/send")
    public ApiResponse<Void> sendResetCode(
            @Valid @RequestBody PasswordResetDto.SendCodeRequest req) {
        passwordResetService.sendResetCode(req);
        return ApiResponse.ok();
    }
    @PostMapping("/password-reset/verify")
    public ApiResponse<Void> verifyCode(
            @Valid @RequestBody PasswordResetDto.VerifyCodeRequest req){
        passwordResetService.verifyCode(req);
        return ApiResponse.ok();
    }
    @PostMapping("/password-reset/reset")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody PasswordResetDto.ResetPasswordRequest req){
        passwordResetService.resetPassword(req);
        return ApiResponse.ok();
    }
}
