-- rename 'newsaper_id' to 'newspaper_table_id' for consistency with other tables
ALTER TABLE chapters
RENAME COLUMN newspaper_id TO newspaper_table_id;