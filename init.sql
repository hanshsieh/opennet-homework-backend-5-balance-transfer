CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(64) NOT NULL PRIMARY KEY,
    balance BIGINT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

CREATE TABLE IF NOT EXISTS transfers (
    id CHAR(36) NOT NULL PRIMARY KEY,
    from_user_id VARCHAR(64) NOT NULL,
    to_user_id VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_transfer_from FOREIGN KEY (from_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_transfer_to FOREIGN KEY (to_user_id) REFERENCES users (user_id),
    INDEX idx_transfer_from (from_user_id),
    INDEX idx_transfer_to (to_user_id),
    INDEX idx_transfer_created (created_at DESC)
);
