-- =========================================================
-- ORDERS
-- =========================================================
CREATE TABLE orders (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        user_id BIGINT NOT NULL,
                        total_amount DECIMAL(15,2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        paid_at DATETIME(6) NULL,
                        PRIMARY KEY (id),
                        KEY idx_orders_user_id (user_id),
                        KEY idx_orders_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- PAYMENTS
-- =========================================================
CREATE TABLE payments (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          order_id BIGINT NOT NULL,
                          payment_key VARCHAR(200) NOT NULL,
                          amount DECIMAL(15,2) NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          PRIMARY KEY (id),
                          UNIQUE KEY uk_payments_payment_key (payment_key),
                          KEY idx_payments_order_id (order_id),
                          CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;