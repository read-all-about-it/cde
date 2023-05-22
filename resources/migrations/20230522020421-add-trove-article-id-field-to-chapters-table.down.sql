-- remove the 'trove_article_id' field from the chapters table
ALTER TABLE chapters
DROP COLUMN trove_article_id;