package com.koala.koalaback.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="password_reset_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String email;
    @Column(nullable = false, length = 64)
    private String token;
    @Column(nullable = false)
    private boolean isUsed;

    /**
     * verifyCode 성공 시 true 로 설정됩니다.
     * resetPassword 는 isVerified = true 인 토큰만 수락해
     * 코드 확인 단계를 건너뛴 직접 재설정 시도를 차단합니다.
     */
    @Column(nullable = false)
    private boolean isVerified;

    @Column(nullable = false)
    private LocalDateTime expiredAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public PasswordResetToken(String email, String token) {
        this.email = email;
        this.token = token;
        this.isUsed = false;
        this.isVerified = false;
        this.expiredAt = LocalDateTime.now().plusMinutes(5);
        this.createdAt = LocalDateTime.now();
    }
    public boolean isExpired(){
        return LocalDateTime.now().isAfter(this.expiredAt);
    }
    public void verify() {
        this.isVerified = true;
    }
    public void use(){
        this.isUsed = true;
    }

}
