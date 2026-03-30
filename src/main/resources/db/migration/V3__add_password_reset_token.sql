CREATE TABLE password_reset_token (
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
