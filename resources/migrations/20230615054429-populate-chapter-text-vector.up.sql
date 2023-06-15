-- set tsvector column to the text of the chapter (for full text search)
UPDATE chapters SET chapter_text_vector = to_tsvector('english', chapter_text);
