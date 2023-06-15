-- create an index on the tsvector column in chapters
CREATE INDEX chapters_text_vector_idx ON chapters USING gin(chapter_text_vector);