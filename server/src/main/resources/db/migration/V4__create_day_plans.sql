CREATE TABLE IF NOT EXISTS day_plans (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    day_number INT NOT NULL,
    date_label VARCHAR(50) NOT NULL
);
