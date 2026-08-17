CREATE TABLE IF NOT EXISTS trip_members (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'member',
    joined_at BIGINT NOT NULL,
    CONSTRAINT uq_trip_members_trip_user UNIQUE (trip_id, user_id)
);
