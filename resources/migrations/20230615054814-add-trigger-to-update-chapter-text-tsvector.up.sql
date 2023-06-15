-- add trigger to update chapter_text_vector column
CREATE TRIGGER chapters_vector_update BEFORE INSERT OR UPDATE
ON chapters FOR EACH ROW EXECUTE PROCEDURE
tsvector_update_trigger(chapter_text_vector, 'pg_catalog.english', chapter_text);
