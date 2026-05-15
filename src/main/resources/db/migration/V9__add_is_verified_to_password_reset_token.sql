-- verifyCode 성공 여부를 추적하는 컬럼 추가
-- resetPassword 는 is_verified = true 인 토큰만 수락해 단계 우회를 방지합니다.
ALTER TABLE password_reset_token
    ADD COLUMN is_verified TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'verifyCode 성공 시 1로 설정. resetPassword는 이 값이 1인 경우만 허용.';
