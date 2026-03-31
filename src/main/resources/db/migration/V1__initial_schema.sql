-- V1__initial_schema.sql

-- USERS
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) UNIQUE,
    apple_id    VARCHAR(255) UNIQUE,
    name        VARCHAR(255),
    device_token VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- SUBSCRIPTIONS
CREATE TABLE subscriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan                VARCHAR(20) NOT NULL,  -- 'TRIAL' | 'MONTHLY' | 'YEARLY'
    status              VARCHAR(20) NOT NULL,  -- 'ACTIVE' | 'EXPIRED' | 'CANCELLED'
    trial_start         TIMESTAMP NOT NULL DEFAULT NOW(),
    trial_end           TIMESTAMP NOT NULL,
    current_period_start TIMESTAMP,
    current_period_end   TIMESTAMP,
    apple_original_transaction_id VARCHAR(255),
    apple_latest_receipt TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- PRAYER LOGS
CREATE TABLE prayer_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    prayer_name VARCHAR(20) NOT NULL,  -- 'FAJR' | 'DHUHR' | 'ASR' | 'MAGHRIB' | 'ISHA'
    prayer_date DATE NOT NULL,
    prayed_at   TIMESTAMP,
    is_done     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, prayer_name, prayer_date)
);

-- STREAKS
CREATE TABLE streaks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    current_streak  INT NOT NULL DEFAULT 0,
    longest_streak  INT NOT NULL DEFAULT 0,
    last_full_day   DATE,
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- NOTIFICATION SETTINGS
CREATE TABLE notification_settings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    offset_minutes  INT NOT NULL DEFAULT 10,
    content_type    VARCHAR(20) NOT NULL DEFAULT 'KARMA',  -- 'HADIS' | 'AYET' | 'KARMA'
    fajr_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    dhuhr_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    asr_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    maghrib_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    isha_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- İNDEKSLER
CREATE INDEX idx_prayer_logs_user_date ON prayer_logs(user_id, prayer_date);
CREATE INDEX idx_prayer_logs_date ON prayer_logs(prayer_date);
CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
