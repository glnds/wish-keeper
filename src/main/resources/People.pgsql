CREATE TYPE behavior_enum AS ENUM ('naughty', 'nice');

CREATE TABLE people (
    id SERIAL PRIMARY KEY,
    firstName VARCHAR(255),
    lastName VARCHAR(255),
    dateOfBirth DATE,
    timeOfRegistration TIMESTAMP,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    version INTEGER,
    behavior behavior_enum DEFAULT 'nice' -- Add behavior field
);
