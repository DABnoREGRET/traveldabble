CREATE TABLE IF NOT EXISTS telemetry (
    id BIGSERIAL PRIMARY KEY,
    timestamp BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(100),
    endpoint VARCHAR(200),
    method VARCHAR(10),
    status_code INT,
    response_time_ms BIGINT,
    user_agent VARCHAR(500),
    ip_address VARCHAR(50),
    metadata TEXT,
    screen_name VARCHAR(100),
    duration_ms BIGINT,
    connection_type VARCHAR(20),
    memory_mb INT,
    exception_hash VARCHAR(64),
    opt_out BOOLEAN NOT NULL DEFAULT FALSE
);
