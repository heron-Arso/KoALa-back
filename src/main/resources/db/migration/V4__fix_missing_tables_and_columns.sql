-- V2, V3이 빈 파일이어서 실제 생성되지 않은 테이블/컬럼을 보완합니다.

-- 1. password_reset_token 테이블 (V3에서 누락됨)
CREATE TABLE IF NOT EXISTS password_reset_token (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    token      VARCHAR(6)   NOT NULL,
    is_used    TINYINT(1)   NOT NULL DEFAULT 0,
    expired_at DATETIME     NOT NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_prt_email       (email),
    INDEX idx_prt_email_token (email, token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. users 테이블 oauth_provider 컬럼 (V2에서 누락됨) — MySQL 8.0 호환 조건부 추가
SET @exist_oauth_provider = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'users'
      AND COLUMN_NAME  = 'oauth_provider'
);
SET @sql_oauth_provider = IF(
    @exist_oauth_provider = 0,
    'ALTER TABLE users ADD COLUMN oauth_provider VARCHAR(20) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql_oauth_provider;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. users 테이블 oauth_id 컬럼 (V2에서 누락됨)
SET @exist_oauth_id = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'users'
      AND COLUMN_NAME  = 'oauth_id'
);
SET @sql_oauth_id = IF(
    @exist_oauth_id = 0,
    'ALTER TABLE users ADD COLUMN oauth_id VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql_oauth_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
