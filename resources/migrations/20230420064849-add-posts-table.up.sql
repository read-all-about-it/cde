CREATE TABLE posts (
  id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  created_by INTEGER NOT NULL REFERENCES users(id),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  latest_version BIGINT REFERENCES versions(id),
  published BOOLEAN NOT NULL DEFAULT FALSE,
  metadata_title TEXT NOT NULL DEFAULT '',
  metadata_authors TEXT NOT NULL DEFAULT '',
  metadata_editors TEXT NOT NULL DEFAULT '',
  metadata_description TEXT NOT NULL DEFAULT '',
  metadata_tags TEXT NOT NULL DEFAULT ''
);
