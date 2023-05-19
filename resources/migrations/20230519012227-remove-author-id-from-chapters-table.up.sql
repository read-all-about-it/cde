-- remove 'author_id' from chapters table (as titles provides the link to authors)
ALTER TABLE chapters
DROP COLUMN IF EXISTS author_id;