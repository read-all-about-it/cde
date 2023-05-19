ALTER TABLE chapters
ADD COLUMN IF NOT EXISTS author_id integer REFERENCES authors(id);