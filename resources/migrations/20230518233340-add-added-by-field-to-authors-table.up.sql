-- add an 'added_by' field to authors table for consistency with other tables
ALTER TABLE authors
ADD COLUMN IF NOT EXISTS added_by INTEGER REFERENCES users(id);