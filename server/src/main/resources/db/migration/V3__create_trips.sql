CREATE TABLE IF NOT EXISTS trips (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    destination VARCHAR(200) NOT NULL,
    country VARCHAR(100) NOT NULL,
    start_date VARCHAR(50) NOT NULL,
    end_date VARCHAR(50) NOT NULL,
    days_until INT,
    cover_colors TEXT NOT NULL,
    travelers INT NOT NULL,
    created_at BIGINT NOT NULL
);
