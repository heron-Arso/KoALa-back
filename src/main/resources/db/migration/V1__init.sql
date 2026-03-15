-- =========================================================
-- 이 파일은 koala_schema_v2.sql 과 동일합니다
-- 파일명: V1__init_schema.sql
-- 경로: src/main/resources/db/migration/V1__init_schema.sql
-- =========================================================

CREATE TABLE users (
                       id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                       user_code       VARCHAR(40)      NOT NULL,
                       email           VARCHAR(255)     NOT NULL,
                       password_hash   VARCHAR(255)     NOT NULL,
                       name            VARCHAR(100)     NOT NULL,
                       phone           VARCHAR(30)      NULL,
                       status          VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
                       last_login_at   DATETIME(3)      NULL,
                       created_at      DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                       updated_at      DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                       deleted_at      DATETIME(3)      NULL,
                       PRIMARY KEY (id),
                       UNIQUE KEY uk_users_user_code (user_code),
                       UNIQUE KEY uk_users_email    (email),
                       KEY idx_users_status (status),
                       CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
                       CONSTRAINT ck_users_phone  CHECK (phone IS NULL OR phone REGEXP '^\\+[1-9][0-9]{6,14}$')
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_addresses (
                                id               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                                user_id          BIGINT UNSIGNED  NOT NULL,
                                label            VARCHAR(50)      NULL,
                                recipient_name   VARCHAR(100)     NOT NULL,
                                recipient_phone  VARCHAR(30)      NOT NULL,
                                zip_code         VARCHAR(20)      NOT NULL,
                                address1         VARCHAR(255)     NOT NULL,
                                address2         VARCHAR(255)     NULL,
                                is_default       BOOLEAN          NOT NULL DEFAULT FALSE,
                                created_at       DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                updated_at       DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                                PRIMARY KEY (id),
                                KEY idx_user_addresses_user_id  (user_id),
                                KEY idx_user_addresses_default  (user_id, is_default),
                                CONSTRAINT fk_user_addresses_user
                                    FOREIGN KEY (user_id) REFERENCES users(id)
                                        ON UPDATE RESTRICT ON DELETE CASCADE,
                                CONSTRAINT ck_user_addresses_phone
                                    CHECK (recipient_phone REGEXP '^\\+[1-9][0-9]{6,14}$')
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE artists (
                         id                 BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                         artist_code        VARCHAR(40)      NOT NULL,
                         name               VARCHAR(150)     NOT NULL,
                         slug               VARCHAR(180)     NOT NULL,
                         description        LONGTEXT         NULL,
                         profile_image_url  VARCHAR(700)     NULL,
                         is_active          BOOLEAN          NOT NULL DEFAULT TRUE,
                         created_at         DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                         updated_at         DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                         deleted_at         DATETIME(3)      NULL,
                         PRIMARY KEY (id),
                         UNIQUE KEY uk_artists_artist_code (artist_code),
                         UNIQUE KEY uk_artists_slug        (slug),
                         KEY idx_artists_name      (name),
                         KEY idx_artists_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE artist_media (
                              id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                              artist_id      BIGINT UNSIGNED  NOT NULL,
                              media_type     VARCHAR(20)      NOT NULL,
                              media_role     VARCHAR(30)      NOT NULL,
                              file_url       VARCHAR(700)     NOT NULL,
                              thumbnail_url  VARCHAR(700)     NULL,
                              title          VARCHAR(200)     NULL,
                              description    TEXT             NULL,
                              sort_order     INT              NOT NULL DEFAULT 0,
                              meta_json      JSON             NULL,
                              created_at     DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                              PRIMARY KEY (id),
                              KEY idx_artist_media_artist_role (artist_id, media_role, sort_order),
                              CONSTRAINT fk_artist_media_artist
                                  FOREIGN KEY (artist_id) REFERENCES artists(id)
                                      ON UPDATE RESTRICT ON DELETE CASCADE,
                              CONSTRAINT ck_artist_media_type CHECK (media_type IN ('IMAGE', 'VIDEO')),
                              CONSTRAINT ck_artist_media_role CHECK (media_role IN ('PROFILE', 'INTERVIEW_IMAGE', 'INTERVIEW_VIDEO', 'GALLERY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE skus (
                      id                    BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                      sku_code              VARCHAR(40)      NOT NULL,
                      artist_id             BIGINT UNSIGNED  NOT NULL,
                      name                  VARCHAR(200)     NOT NULL,
                      slug                  VARCHAR(220)     NOT NULL,
                      description           LONGTEXT         NULL,
                      sku_type              VARCHAR(20)      NOT NULL DEFAULT 'ARTWORK',
                      genre                 VARCHAR(50)      NOT NULL DEFAULT 'ART_TOY',
                      currency              CHAR(3)          NOT NULL DEFAULT 'KRW',
                      list_price            DECIMAL(13,2)    NOT NULL DEFAULT 0.00,
                      sale_price            DECIMAL(13,2)    NULL,
                      is_limited_edition    BOOLEAN          NOT NULL DEFAULT FALSE,
                      edition_size          INT UNSIGNED     NULL,
                      edition_number        INT UNSIGNED     NULL,
                      primary_image_url     VARCHAR(700)     NULL,
                      ar_asset_url          VARCHAR(700)     NULL,
                      ar_preview_image_url  VARCHAR(700)     NULL,
                      spine_pictures_json   JSON             NULL,
                      width_cm              DECIMAL(10,2)    NULL,
                      height_cm             DECIMAL(10,2)    NULL,
                      depth_cm              DECIMAL(10,2)    NULL,
                      weight_kg             DECIMAL(10,3)    NULL,
                      status                VARCHAR(20)      NOT NULL DEFAULT 'DRAFT',
                      published_at          DATETIME(3)      NULL,
                      created_at            DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                      updated_at            DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                      deleted_at            DATETIME(3)      NULL,
                      PRIMARY KEY (id),
                      UNIQUE KEY uk_skus_sku_code (sku_code),
                      UNIQUE KEY uk_skus_slug     (slug),
                      KEY idx_skus_artist_id    (artist_id),
                      KEY idx_skus_status       (status),
                      KEY idx_skus_genre        (genre),
                      KEY idx_skus_published_at (published_at),
                      CONSTRAINT fk_skus_artist
                          FOREIGN KEY (artist_id) REFERENCES artists(id)
                              ON UPDATE RESTRICT ON DELETE RESTRICT,
                      CONSTRAINT ck_skus_type   CHECK (sku_type IN ('ARTWORK', 'GOODS')),
                      CONSTRAINT ck_skus_status CHECK (status IN ('DRAFT', 'ACTIVE', 'OUT_OF_STOCK', 'DISCONTINUED')),
                      CONSTRAINT ck_skus_prices CHECK (
                          list_price >= 0
                              AND (sale_price IS NULL OR sale_price >= 0)
                              AND (sale_price IS NULL OR sale_price <= list_price)
                          ),
                      CONSTRAINT ck_skus_edition CHECK (
                          (is_limited_edition = FALSE AND edition_size IS NULL AND edition_number IS NULL)
                              OR (is_limited_edition = TRUE
                              AND edition_size IS NOT NULL AND edition_size > 0
                              AND edition_number IS NOT NULL AND edition_number > 0
                              AND edition_number <= edition_size)
                          )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sku_stock_ledger (
                                  id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                                  sku_id      BIGINT UNSIGNED  NOT NULL,
                                  delta       INT              NOT NULL,
                                  reason      VARCHAR(30)      NOT NULL,
                                  ref_type    VARCHAR(30)      NULL,
                                  ref_id      BIGINT UNSIGNED  NULL,
                                  memo        VARCHAR(200)     NULL,
                                  created_at  DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                  PRIMARY KEY (id),
                                  KEY idx_sku_stock_ledger_sku_id (sku_id, created_at),
                                  KEY idx_sku_stock_ledger_ref    (ref_type, ref_id),
                                  CONSTRAINT fk_sku_stock_ledger_sku
                                      FOREIGN KEY (sku_id) REFERENCES skus(id)
                                          ON UPDATE RESTRICT ON DELETE RESTRICT,
                                  CONSTRAINT ck_sku_stock_ledger_reason
                                      CHECK (reason IN ('INITIAL', 'PURCHASE', 'CANCEL_RESTORE', 'ADMIN_ADJUST', 'RETURN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sku_review_stats (
                                  sku_id       BIGINT UNSIGNED  NOT NULL,
                                  review_count INT UNSIGNED     NOT NULL DEFAULT 0,
                                  rating_sum   BIGINT UNSIGNED  NOT NULL DEFAULT 0,
                                  avg_rating   DECIMAL(3,2)     NOT NULL DEFAULT 0.00,
                                  updated_at   DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                                  PRIMARY KEY (sku_id),
                                  CONSTRAINT fk_sku_review_stats_sku
                                      FOREIGN KEY (sku_id) REFERENCES skus(id)
                                          ON UPDATE RESTRICT ON DELETE CASCADE,
                                  CONSTRAINT ck_sku_review_stats_avg CHECK (avg_rating >= 0 AND avg_rating <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sku_media (
                           id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                           sku_id         BIGINT UNSIGNED  NOT NULL,
                           media_type     VARCHAR(20)      NOT NULL,
                           media_role     VARCHAR(30)      NOT NULL,
                           file_url       VARCHAR(700)     NOT NULL,
                           thumbnail_url  VARCHAR(700)     NULL,
                           alt_text       VARCHAR(255)     NULL,
                           sort_order     INT              NOT NULL DEFAULT 0,
                           angle_degree   DECIMAL(6,2)     NULL,
                           is_primary     BOOLEAN          NOT NULL DEFAULT FALSE,
                           meta_json      JSON             NULL,
                           created_at     DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                           PRIMARY KEY (id),
                           KEY idx_sku_media_sku     (sku_id),
                           KEY idx_sku_media_role    (sku_id, media_role, sort_order),
                           KEY idx_sku_media_primary (sku_id, is_primary),
                           CONSTRAINT fk_sku_media_sku
                               FOREIGN KEY (sku_id) REFERENCES skus(id)
                                   ON UPDATE RESTRICT ON DELETE CASCADE,
                           CONSTRAINT ck_sku_media_type CHECK (media_type IN ('IMAGE', 'VIDEO', 'MODEL_3D')),
                           CONSTRAINT ck_sku_media_role CHECK (media_role IN ('MAIN', 'DETAIL', 'GALLERY', 'SPINE_360', 'AR_PREVIEW', 'AR_MODEL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE carts (
                       id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                       user_id     BIGINT UNSIGNED  NOT NULL,
                       currency    CHAR(3)          NOT NULL DEFAULT 'KRW',
                       created_at  DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                       updated_at  DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                       PRIMARY KEY (id),
                       UNIQUE KEY uk_carts_user_id (user_id),
                       CONSTRAINT fk_carts_user
                           FOREIGN KEY (user_id) REFERENCES users(id)
                               ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cart_items (
                            id                    BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                            cart_id               BIGINT UNSIGNED  NOT NULL,
                            sku_id                BIGINT UNSIGNED  NOT NULL,
                            quantity              INT UNSIGNED     NOT NULL DEFAULT 1,
                            unit_price            DECIMAL(13,2)    NOT NULL,
                            option_snapshot_json  JSON             NULL,
                            added_at              DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                            updated_at            DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_cart_items_cart_sku (cart_id, sku_id),
                            KEY idx_cart_items_sku_id (sku_id),
                            CONSTRAINT fk_cart_items_cart
                                FOREIGN KEY (cart_id) REFERENCES carts(id)
                                    ON UPDATE RESTRICT ON DELETE CASCADE,
                            CONSTRAINT fk_cart_items_sku
                                FOREIGN KEY (sku_id) REFERENCES skus(id)
                                    ON UPDATE RESTRICT ON DELETE RESTRICT,
                            CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0),
                            CONSTRAINT ck_cart_items_price    CHECK (unit_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wishlist_items (
                                id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                                user_id     BIGINT UNSIGNED  NOT NULL,
                                sku_id      BIGINT UNSIGNED  NOT NULL,
                                created_at  DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                PRIMARY KEY (id),
                                UNIQUE KEY uk_wishlist_items_user_sku (user_id, sku_id),
                                KEY idx_wishlist_items_sku_id (sku_id),
                                CONSTRAINT fk_wishlist_items_user
                                    FOREIGN KEY (user_id) REFERENCES users(id)
                                        ON UPDATE RESTRICT ON DELETE CASCADE,
                                CONSTRAINT fk_wishlist_items_sku
                                    FOREIGN KEY (sku_id) REFERENCES skus(id)
                                        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE orders (
                        id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                        order_no        VARCHAR(40)      NOT NULL,
                        user_id         BIGINT UNSIGNED  NOT NULL,
                        order_status    VARCHAR(30)      NOT NULL DEFAULT 'PENDING_PAYMENT',
                        payment_status  VARCHAR(30)      NOT NULL DEFAULT 'READY',
                        currency        CHAR(3)          NOT NULL DEFAULT 'KRW',
                        product_amount  DECIMAL(13,2)    NOT NULL DEFAULT 0.00,
                        discount_amount DECIMAL(13,2)    NOT NULL DEFAULT 0.00,
                        shipping_amount DECIMAL(13,2)    NOT NULL DEFAULT 0.00,
                        tax_amount      DECIMAL(13,2)    NOT NULL DEFAULT 0.00,
                        total_amount    DECIMAL(13,2)    NOT NULL DEFAULT 0.00,
                        orderer_name    VARCHAR(100)     NOT NULL,
                        orderer_email   VARCHAR(255)     NOT NULL,
                        orderer_phone   VARCHAR(30)      NOT NULL,
                        paid_at         DATETIME(3)      NULL,
                        cancelled_at    DATETIME(3)      NULL,
                        created_at      DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                        updated_at      DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_orders_order_no (order_no),
                        KEY idx_orders_user_id        (user_id),
                        KEY idx_orders_order_status   (order_status),
                        KEY idx_orders_payment_status (payment_status),
                        KEY idx_orders_created_at     (created_at),
                        CONSTRAINT fk_orders_user
                            FOREIGN KEY (user_id) REFERENCES users(id)
                                ON UPDATE RESTRICT ON DELETE RESTRICT,
                        CONSTRAINT ck_orders_order_status CHECK (order_status IN (
                                                                                  'PENDING_PAYMENT','PAID','PREPARING','SHIPPED',
                                                                                  'DELIVERED','CANCELLED','REFUNDED','PARTIAL_REFUNDED')),
                        CONSTRAINT ck_orders_payment_status CHECK (payment_status IN (
                                                                                      'READY','PAID','FAILED','CANCELLED','REFUNDED','PARTIAL_REFUNDED')),
                        CONSTRAINT ck_orders_amounts CHECK (
                            product_amount  >= 0 AND discount_amount >= 0
                                AND shipping_amount >= 0 AND tax_amount >= 0 AND total_amount >= 0
                            )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_shipments (
                                 id                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                                 order_id          BIGINT UNSIGNED  NOT NULL,
                                 recipient_name    VARCHAR(100)     NOT NULL,
                                 recipient_phone   VARCHAR(30)      NOT NULL,
                                 zip_code          VARCHAR(20)      NOT NULL,
                                 address1          VARCHAR(255)     NOT NULL,
                                 address2          VARCHAR(255)     NULL,
                                 delivery_request  VARCHAR(255)     NULL,
                                 carrier_code      VARCHAR(50)      NULL,
                                 tracking_no       VARCHAR(100)     NULL,
                                 shipped_at        DATETIME(3)      NULL,
                                 delivered_at      DATETIME(3)      NULL,
                                 created_at        DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                 updated_at        DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                                 PRIMARY KEY (id),
                                 KEY idx_order_shipments_order_id (order_id),
                                 KEY idx_order_shipments_tracking (carrier_code, tracking_no),
                                 CONSTRAINT fk_order_shipments_order
                                     FOREIGN KEY (order_id) REFERENCES orders(id)
                                         ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_items (
                             id                   BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                             order_id             BIGINT UNSIGNED  NOT NULL,
                             sku_id               BIGINT UNSIGNED  NULL,
                             artist_id            BIGINT UNSIGNED  NULL,
                             sku_code_snapshot    VARCHAR(40)      NOT NULL,
                             artist_code_snapshot VARCHAR(40)      NULL,
                             sku_name_snapshot    VARCHAR(200)     NOT NULL,
                             artist_name_snapshot VARCHAR(150)     NULL,
                             quantity             INT UNSIGNED     NOT NULL DEFAULT 1,
                             unit_price           DECIMAL(13,2)    NOT NULL,
                             discount_amount      DECIMAL(13,2)    NOT NULL DEFAULT 0.00,
                             tax_amount           DECIMAL(13,2)    NOT NULL DEFAULT 0.00,
                             line_total_amount    DECIMAL(13,2)    NOT NULL,
                             sku_snapshot_json    JSON             NULL,
                             review_written       BOOLEAN          NOT NULL DEFAULT FALSE,
                             created_at           DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                             PRIMARY KEY (id),
                             KEY idx_order_items_order_id  (order_id),
                             KEY idx_order_items_sku_id    (sku_id),
                             KEY idx_order_items_artist_id (artist_id),
                             CONSTRAINT fk_order_items_order
                                 FOREIGN KEY (order_id) REFERENCES orders(id)
                                     ON UPDATE RESTRICT ON DELETE CASCADE,
                             CONSTRAINT fk_order_items_sku
                                 FOREIGN KEY (sku_id) REFERENCES skus(id)
                                     ON UPDATE RESTRICT ON DELETE SET NULL,
                             CONSTRAINT fk_order_items_artist
                                 FOREIGN KEY (artist_id) REFERENCES artists(id)
                                     ON UPDATE RESTRICT ON DELETE SET NULL,
                             CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
                             CONSTRAINT ck_order_items_amounts CHECK (
                                 unit_price >= 0 AND discount_amount >= 0
                                     AND tax_amount >= 0 AND line_total_amount >= 0
                                 )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payments (
                          id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                          order_id            BIGINT UNSIGNED  NOT NULL,
                          payment_no          VARCHAR(40)      NOT NULL,
                          provider            VARCHAR(30)      NOT NULL,
                          method              VARCHAR(30)      NOT NULL,
                          status              VARCHAR(30)      NOT NULL DEFAULT 'READY',
                          requested_amount    DECIMAL(13,2)    NOT NULL DEFAULT 0.00,
                          approved_amount     DECIMAL(13,2)    NOT NULL DEFAULT 0.00,
                          cancelled_amount    DECIMAL(13,2)    NOT NULL DEFAULT 0.00,
                          currency            CHAR(3)          NOT NULL DEFAULT 'KRW',
                          pg_transaction_id   VARCHAR(100)     NULL,
                          approval_no         VARCHAR(100)     NULL,
                          failure_code        VARCHAR(100)     NULL,
                          failure_message     VARCHAR(255)     NULL,
                          approved_at         DATETIME(3)      NULL,
                          failed_at           DATETIME(3)      NULL,
                          cancelled_at        DATETIME(3)      NULL,
                          raw_response_json   JSON             NULL,
                          created_at          DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                          updated_at          DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                          PRIMARY KEY (id),
                          UNIQUE KEY uk_payments_payment_no        (payment_no),
                          UNIQUE KEY uk_payments_pg_transaction_id (pg_transaction_id),
                          KEY idx_payments_order_id (order_id),
                          KEY idx_payments_status   (status),
                          CONSTRAINT fk_payments_order
                              FOREIGN KEY (order_id) REFERENCES orders(id)
                                  ON UPDATE RESTRICT ON DELETE RESTRICT,
                          CONSTRAINT ck_payments_provider CHECK (provider IN (
                                                                              'TOSS','KG_INICIS','KAKAOPAY','NAVERPAY','STRIPE','PAYPAL','ETC')),
                          CONSTRAINT ck_payments_method CHECK (method IN (
                                                                          'CARD','BANK_TRANSFER','VIRTUAL_ACCOUNT','KAKAOPAY','NAVERPAY','POINT','MOBILE','ETC')),
                          CONSTRAINT ck_payments_status CHECK (status IN (
                                                                          'READY','AUTHORIZED','CAPTURED','FAILED','CANCELLED','REFUNDED','PARTIAL_REFUNDED')),
                          CONSTRAINT ck_payments_amounts CHECK (
                              requested_amount >= 0 AND approved_amount >= 0 AND cancelled_amount >= 0
                              )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payment_events (
                                id                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                                payment_id        BIGINT UNSIGNED  NOT NULL,
                                event_type        VARCHAR(30)      NOT NULL,
                                event_status      VARCHAR(20)      NOT NULL,
                                amount            DECIMAL(13,2)    NOT NULL DEFAULT 0.00,
                                provider_event_id VARCHAR(100)     NULL,
                                payload_json      JSON             NULL,
                                created_at        DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                PRIMARY KEY (id),
                                KEY idx_payment_events_payment_id   (payment_id),
                                KEY idx_payment_events_type_created (event_type, created_at),
                                CONSTRAINT fk_payment_events_payment
                                    FOREIGN KEY (payment_id) REFERENCES payments(id)
                                        ON UPDATE RESTRICT ON DELETE CASCADE,
                                CONSTRAINT ck_payment_events_type CHECK (event_type IN (
                                                                                        'READY','AUTH_REQUESTED','AUTHORIZED','CAPTURE_REQUESTED',
                                                                                        'CAPTURED','WEBHOOK','CANCELLED','REFUNDED','FAILED')),
                                CONSTRAINT ck_payment_events_status CHECK (event_status IN ('SUCCESS','FAILED')),
                                CONSTRAINT ck_payment_events_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admins (
                        id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                        admin_code          VARCHAR(40)      NOT NULL,
                        login_id            VARCHAR(50)      NOT NULL,
                        password_hash       VARCHAR(255)     NOT NULL,
                        name                VARCHAR(100)     NOT NULL,
                        email               VARCHAR(150)     NULL,
                        phone               VARCHAR(30)      NULL,
                        status              VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
                        login_fail_count    INT              NOT NULL DEFAULT 0,
                        last_login_at       DATETIME(3)      NULL,
                        last_login_ip       VARCHAR(45)      NULL,
                        password_changed_at DATETIME(3)      NULL,
                        created_at          DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                        updated_at          DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                        deleted_at          DATETIME(3)      NULL,
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_admins_admin_code (admin_code),
                        UNIQUE KEY uk_admins_login_id   (login_id),
                        UNIQUE KEY uk_admins_email      (email),
                        KEY idx_admins_status     (status),
                        KEY idx_admins_created_at (created_at),
                        CONSTRAINT ck_admins_status CHECK (status IN ('ACTIVE','INACTIVE','LOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_roles (
                             id           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                             role_code    VARCHAR(50)      NOT NULL,
                             role_name    VARCHAR(100)     NOT NULL,
                             description  VARCHAR(255)     NULL,
                             is_active    BOOLEAN          NOT NULL DEFAULT TRUE,
                             created_at   DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                             updated_at   DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                             PRIMARY KEY (id),
                             UNIQUE KEY uk_admin_roles_role_code (role_code),
                             KEY idx_admin_roles_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_role_mappings (
                                     id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                                     admin_id    BIGINT UNSIGNED  NOT NULL,
                                     role_id     BIGINT UNSIGNED  NOT NULL,
                                     created_at  DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                     PRIMARY KEY (id),
                                     UNIQUE KEY uk_admin_role_mappings_admin_role (admin_id, role_id),
                                     KEY idx_admin_role_mappings_admin_id (admin_id),
                                     KEY idx_admin_role_mappings_role_id  (role_id),
                                     CONSTRAINT fk_admin_role_mappings_admin
                                         FOREIGN KEY (admin_id) REFERENCES admins(id)
                                             ON UPDATE RESTRICT ON DELETE CASCADE,
                                     CONSTRAINT fk_admin_role_mappings_role
                                         FOREIGN KEY (role_id) REFERENCES admin_roles(id)
                                             ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_audit_logs (
                                  id           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                                  admin_id     BIGINT UNSIGNED  NULL,
                                  action_type  VARCHAR(50)      NOT NULL,
                                  target_type  VARCHAR(50)      NOT NULL,
                                  target_id    BIGINT UNSIGNED  NULL,
                                  request_path VARCHAR(255)     NULL,
                                  http_method  VARCHAR(10)      NULL,
                                  ip_address   VARCHAR(45)      NULL,
                                  user_agent   VARCHAR(500)     NULL,
                                  before_data  JSON             NULL,
                                  after_data   JSON             NULL,
                                  memo         VARCHAR(500)     NULL,
                                  created_at   DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                  PRIMARY KEY (id),
                                  KEY idx_admin_audit_logs_admin_id              (admin_id),
                                  KEY idx_admin_audit_logs_action_type           (action_type),
                                  KEY idx_admin_audit_logs_target_type_target_id (target_type, target_id),
                                  KEY idx_admin_audit_logs_created_at            (created_at),
                                  CONSTRAINT fk_admin_audit_logs_admin
                                      FOREIGN KEY (admin_id) REFERENCES admins(id)
                                          ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE banners (
                         id                    BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                         banner_code           VARCHAR(40)      NOT NULL,
                         banner_type           VARCHAR(30)      NOT NULL DEFAULT 'MAIN',
                         title                 VARCHAR(200)     NOT NULL,
                         subtitle              VARCHAR(255)     NULL,
                         image_url             VARCHAR(700)     NOT NULL,
                         mobile_image_url      VARCHAR(700)     NULL,
                         link_url              VARCHAR(700)     NULL,
                         link_target           VARCHAR(20)      NOT NULL DEFAULT 'SELF',
                         bg_color              VARCHAR(30)      NULL,
                         text_color            VARCHAR(30)      NULL,
                         sort_order            INT              NOT NULL DEFAULT 0,
                         is_active             BOOLEAN          NOT NULL DEFAULT TRUE,
                         visible_from          DATETIME(3)      NULL,
                         visible_to            DATETIME(3)      NULL,
                         created_by_admin_id   BIGINT UNSIGNED  NULL,
                         updated_by_admin_id   BIGINT UNSIGNED  NULL,
                         created_at            DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                         updated_at            DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                         deleted_at            DATETIME(3)      NULL,
                         PRIMARY KEY (id),
                         UNIQUE KEY uk_banners_banner_code (banner_code),
                         KEY idx_banners_type_active_sort (banner_type, is_active, sort_order),
                         KEY idx_banners_visible_period   (visible_from, visible_to),
                         CONSTRAINT fk_banners_created_by_admin
                             FOREIGN KEY (created_by_admin_id) REFERENCES admins(id)
                                 ON UPDATE RESTRICT ON DELETE SET NULL,
                         CONSTRAINT fk_banners_updated_by_admin
                             FOREIGN KEY (updated_by_admin_id) REFERENCES admins(id)
                                 ON UPDATE RESTRICT ON DELETE SET NULL,
                         CONSTRAINT ck_banners_type        CHECK (banner_type IN ('MAIN','SUB','EVENT','PROMOTION','ARTIST')),
                         CONSTRAINT ck_banners_link_target CHECK (link_target IN ('SELF','BLANK'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sku_reviews (
                             id                    BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                             review_code           VARCHAR(40)      NOT NULL,
                             order_item_id         BIGINT UNSIGNED  NOT NULL,
                             order_id              BIGINT UNSIGNED  NOT NULL,
                             sku_id                BIGINT UNSIGNED  NOT NULL,
                             user_id               BIGINT UNSIGNED  NOT NULL,
                             rating                TINYINT UNSIGNED NOT NULL,
                             title                 VARCHAR(200)     NULL,
                             content               TEXT             NOT NULL,
                             review_status         VARCHAR(20)      NOT NULL DEFAULT 'PENDING',
                             is_visible            BOOLEAN          NOT NULL DEFAULT FALSE,
                             is_featured           BOOLEAN          NOT NULL DEFAULT FALSE,
                             like_count            INT UNSIGNED     NOT NULL DEFAULT 0,
                             report_count          INT UNSIGNED     NOT NULL DEFAULT 0,
                             admin_memo            VARCHAR(500)     NULL,
                             moderated_by_admin_id BIGINT UNSIGNED  NULL,
                             moderated_at          DATETIME(3)      NULL,
                             created_at            DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                             updated_at            DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                             deleted_at            DATETIME(3)      NULL,
                             PRIMARY KEY (id),
                             UNIQUE KEY uk_sku_reviews_review_code   (review_code),
                             UNIQUE KEY uk_sku_reviews_order_item_id (order_item_id),
                             KEY idx_sku_reviews_sku_status_created  (sku_id, review_status, created_at),
                             KEY idx_sku_reviews_user_id             (user_id),
                             KEY idx_sku_reviews_order_id            (order_id),
                             KEY idx_sku_reviews_rating              (rating),
                             KEY idx_sku_reviews_featured            (is_featured),
                             CONSTRAINT fk_sku_reviews_order_item
                                 FOREIGN KEY (order_item_id) REFERENCES order_items(id)
                                     ON UPDATE RESTRICT ON DELETE RESTRICT,
                             CONSTRAINT fk_sku_reviews_order
                                 FOREIGN KEY (order_id) REFERENCES orders(id)
                                     ON UPDATE RESTRICT ON DELETE RESTRICT,
                             CONSTRAINT fk_sku_reviews_sku
                                 FOREIGN KEY (sku_id) REFERENCES skus(id)
                                     ON UPDATE RESTRICT ON DELETE RESTRICT,
                             CONSTRAINT fk_sku_reviews_user
                                 FOREIGN KEY (user_id) REFERENCES users(id)
                                     ON UPDATE RESTRICT ON DELETE RESTRICT,
                             CONSTRAINT fk_sku_reviews_moderated_admin
                                 FOREIGN KEY (moderated_by_admin_id) REFERENCES admins(id)
                                     ON UPDATE RESTRICT ON DELETE SET NULL,
                             CONSTRAINT ck_sku_reviews_rating CHECK (rating BETWEEN 1 AND 5),
                             CONSTRAINT ck_sku_reviews_status CHECK (review_status IN ('PENDING','APPROVED','HIDDEN','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sku_review_media (
                                  id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                                  review_id      BIGINT UNSIGNED  NOT NULL,
                                  media_type     VARCHAR(20)      NOT NULL,
                                  file_url       VARCHAR(700)     NOT NULL,
                                  thumbnail_url  VARCHAR(700)     NULL,
                                  sort_order     INT              NOT NULL DEFAULT 0,
                                  created_at     DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                  PRIMARY KEY (id),
                                  KEY idx_sku_review_media_review_id (review_id, sort_order),
                                  CONSTRAINT fk_sku_review_media_review
                                      FOREIGN KEY (review_id) REFERENCES sku_reviews(id)
                                          ON UPDATE RESTRICT ON DELETE CASCADE,
                                  CONSTRAINT ck_sku_review_media_type CHECK (media_type IN ('IMAGE','VIDEO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- SEED: ADMIN ROLES
-- =========================================================
INSERT INTO admin_roles (role_code, role_name, description, is_active)
VALUES
    ('SUPER_ADMIN',     '슈퍼 관리자',   '전체 권한',                 TRUE),
    ('CONTENT_MANAGER', '콘텐츠 관리자', '배너/아티스트/콘텐츠 관리', TRUE),
    ('SKU_MANAGER',     'SKU 관리자',    'SKU 및 미디어 관리',        TRUE),
    ('ORDER_MANAGER',   '주문 관리자',   '주문/결제/배송 관리',       TRUE),
    ('REVIEW_MANAGER',  '후기 관리자',   '후기 승인/숨김/반려 관리',  TRUE)
    AS new
ON DUPLICATE KEY UPDATE
                     role_name   = new.role_name,
                     description = new.description,
                     is_active   = new.is_active,
                     updated_at  = CURRENT_TIMESTAMP(3);