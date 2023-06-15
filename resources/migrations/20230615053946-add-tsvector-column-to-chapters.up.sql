-- add 'tsvector' column to chapters table
ALTER TABLE chapters ADD COLUMN chapter_text_vector tsvector;
