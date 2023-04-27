-- Ensures that "last_login_at" and "updated_at" default to NOW()
ALTER TABLE users
ALTER COLUMN last_login_at SET DEFAULT NOW(),
ALTER COLUMN updated_at SET DEFAULT NOW();