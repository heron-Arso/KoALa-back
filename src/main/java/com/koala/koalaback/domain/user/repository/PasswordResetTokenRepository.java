package com.koala.koalaback.domain.user.repository;

import com.koala.koalaback.domain.user.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** verifyCode 단계 — 아직 확인 안 된(isVerified=false) 유효 토큰 조회 */
    Optional<PasswordResetToken> findTopByEmailAndTokenAndIsUsedFalseAndIsVerifiedFalseOrderByCreatedAtDesc(
            String email, String token
    );

    /** resetPassword 단계 — 코드 확인 완료(isVerified=true) 토큰만 수락 */
    Optional<PasswordResetToken> findTopByEmailAndTokenAndIsUsedFalseAndIsVerifiedTrueOrderByCreatedAtDesc(
            String email, String token
    );

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.email = :email")
    void deleteAllByEmail(String email);
}
