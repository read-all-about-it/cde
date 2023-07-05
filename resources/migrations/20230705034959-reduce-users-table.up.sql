-- remove almost all rows in users table (as using auth0 external for auth)
-- leaves only id & email. we get email from auth0 and id is needed for foreign key.
ALTER TABLE users
DROP COLUMN updated_at,
DROP COLUMN created_at,
DROP COLUMN last_login_at,
DROP COLUMN blocked_at,
DROP COLUMN admin,
DROP COLUMN password,
DROP COLUMN username;