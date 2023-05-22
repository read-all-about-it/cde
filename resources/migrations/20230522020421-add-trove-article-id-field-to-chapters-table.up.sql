-- add a 'trove_article_id' field to keep a record of the article id that *trove* uses
ALTER TABLE chapters
ADD COLUMN trove_article_id INTEGER DEFAULT NULL;