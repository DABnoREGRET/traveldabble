CREATE TABLE IF NOT EXISTS destinations (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    country VARCHAR(100) NOT NULL,
    tagline TEXT NOT NULL,
    rating DOUBLE PRECISION NOT NULL,
    tags TEXT NOT NULL,
    cover_colors TEXT NOT NULL
);
