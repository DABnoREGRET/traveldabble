CREATE TABLE IF NOT EXISTS invite_codes (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL UNIQUE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL,
    expires_at BIGINT,
    max_uses INT,
    use_count INT NOT NULL DEFAULT 0
);
