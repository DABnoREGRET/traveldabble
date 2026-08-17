CREATE TABLE IF NOT EXISTS user_fcm_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    token VARCHAR(500) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    created_at BIGINT NOT NULL
);
