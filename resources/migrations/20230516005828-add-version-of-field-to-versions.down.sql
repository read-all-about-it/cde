-- remove the 'version_of' column from the 'versions' table
ALTER TABLE versions
DROP COLUMN version_of;