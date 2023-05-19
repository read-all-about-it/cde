ALTER TABLE chapters
ADD COLUMN IF NOT EXISTS newspaper_table_id integer REFERENCES newspapers(id);