ALTER TABLE newspapers
DROP COLUMN IF EXISTS "created_at",
DROP COLUMN IF EXISTS "updated_at",
DROP COLUMN IF EXISTS "added_by";