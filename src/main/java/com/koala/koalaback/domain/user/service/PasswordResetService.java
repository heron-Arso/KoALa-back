package com.koala.koalaback.domain.user.service;

import com.koala.koalaback.domain.user.dto.PasswordResetDto;
import com.koala.koalaback.domain.user.entity.PasswordResetToken;
import com.koala.koalaback.domain.user.entity.User;
import com.koala.koalaback.domain.user.repository.PasswordResetTokenRepository;
import com.koala.koalaback.domain.user.repository.UserRepository;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.infra.mail.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // ── 인증 코드 발송 ────────────────────────────────────

    @Transactional
    public void sendResetCode(PasswordResetDto.SendCodeRequest req) {
        // 이메일 존재 여부와 무관하게 항상 동일한 응답 반환 (계정 열거 공격 방지)
        // 실제 이메일 전송은 계정이 있을 때만 수행
        userRepository.findByEmail(req.getEmail()).ifPresent(user -> {
            // 소셜 로그인 계정은 비밀번호 재설정 불필요 — 조용히 무시
            if (user.getOauthProvider() != null) return;

            tokenRepository.deleteAllByEmail(req.getEmail());
            String token = generateCode();
            tokenRepository.save(
                    PasswordResetToken.builder()
                            .email(req.getEmail())
                            .token(token)
                            .build()
            );
            emailService.sendPasswordResetEmail(req.getEmail(), token);
        });
        // 미가입 이메일이어도 "인증 코드를 발송했습니다" 응답은 동일 (프론트에서 표시)
    }

    // ── 인증 코드 확인 ────────────────────────────────────

    @Transactional
    public void verifyCode(PasswordResetDto.VerifyCodeRequest req) {
        // isVerified=false 인 토큰만 조회 (이미 확인된 토큰 재사용 방지)
        PasswordResetToken resetToken = tokenRepository
                .findTopByEmailAndTokenAndIsUsedFalseAndIsVerifiedFalseOrderByCreatedAtDesc(
                        req.getEmail(), req.getToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        if (resetToken.isExpired()) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }

        // 코드 확인 완료 표시 — 이후 resetPassword에서만 사용 가능
        resetToken.verify();
    }

    // ── 비밀번호 재설정 ───────────────────────────────────

    @Transactional
    public void resetPassword(PasswordResetDto.ResetPasswordRequest req) {
        // isVerified=true 인 토큰만 수락 — verifyCode를 거치지 않은 직접 재설정 시도 차단
        PasswordResetToken resetToken = tokenRepository
                .findTopByEmailAndTokenAndIsUsedFalseAndIsVerifiedTrueOrderByCreatedAtDesc(
                        req.getEmail(), req.getToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        if (resetToken.isExpired()) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(req.getNewPassword()));
        resetToken.use();
        tokenRepository.deleteAllByEmail(req.getEmail());
    }

    // ── 코드 생성 ─────────────────────────────────────────
    // 6자리 숫자(10^6 = 100만 가지)에서 8자리 영숫자(36^8 ≈ 2.8조 가지)로 변경
    // 브루트포스 공격 저항성 대폭 향상

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TOKEN_LENGTH = 8;

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}