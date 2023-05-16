-- add the 'version_of' column to the 'versions' table
ALTER TABLE versions
ADD COLUMN version_of INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE;