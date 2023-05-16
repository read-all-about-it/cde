-- add the 'created_at' column to the 'versions' table
ALTER TABLE versions
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();