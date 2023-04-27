-- Resets the default values for "last_login_at" and "updated_at" to NULL
ALTER TABLE users
ALTER COLUMN last_login_at DROP DEFAULT,
ALTER COLUMN updated_at DROP DEFAULT;