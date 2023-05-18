-- add "created_at", "updated_at", and "added_by" columns to the newspapers table (for consistency with the titles and chapters tables)
ALTER TABLE newspapers
ADD COLUMN IF NOT EXISTS "created_at" TIMESTAMP DEFAULT NOW(),
ADD COLUMN IF NOT EXISTS "updated_at" TIMESTAMP DEFAULT NOW(),
ADD COLUMN IF NOT EXISTS "added_by" INTEGER REFERENCES users(id);