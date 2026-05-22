CREATE TABLE notices (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    notice_code         VARCHAR(40)  NOT NULL,
    title               VARCHAR(200) NOT NULL,
    content             TEXT         NOT NULL,
    is_pinned           BIT(1)       NOT NULL DEFAULT 0,
    is_active           BIT(1)       NOT NULL DEFAULT 1,
    created_by_admin_id BIGINT,
    updated_by_admin_id BIGINT,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    deleted_at          DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_notices_notice_code (notice_code),
    CONSTRAINT fk_notices_created_by FOREIGN KEY (created_by_admin_id) REFERENCES admins (id) ON DELETE SET NULL,
    CONSTRAINT fk_notices_updated_by FOREIGN KEY (updated_by_admin_id) REFERENCES admins (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
