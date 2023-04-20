CREATE TABLE collection_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    collection_id BIGINT NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    chapter_id INTEGER NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    UNIQUE (collection_id, chapter_id), -- can't have a chapter in a collection twice
    UNIQUE (collection_id, position) -- can't have two items in the same position
);
