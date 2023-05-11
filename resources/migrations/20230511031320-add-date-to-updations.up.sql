-- add a 'created_at' column to the 'updations' table
ALTER TABLE updations
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();