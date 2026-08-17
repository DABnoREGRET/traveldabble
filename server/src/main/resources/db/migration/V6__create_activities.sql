CREATE TABLE IF NOT EXISTS activities (
    id UUID PRIMARY KEY,
    day_plan_id UUID NOT NULL REFERENCES day_plans(id) ON DELETE CASCADE,
    place_id UUID NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    start_time VARCHAR(20) NOT NULL,
    end_time VARCHAR(20) NOT NULL,
    note TEXT
);
