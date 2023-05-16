-- remove the 'created_at' column from the 'versions' table
ALTER TABLE versions
DROP COLUMN created_at;