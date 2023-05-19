-- remove the 'newspaper_table_id' column from the 'chapters' table (it can be joined from the 'titles' table)
ALTER TABLE chapters
DROP COLUMN IF EXISTS newspaper_table_id;