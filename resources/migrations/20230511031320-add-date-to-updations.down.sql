-- remove the 'created_at' column from the 'updations' table
ALTER TABLE updations
DROP COLUMN created_at;