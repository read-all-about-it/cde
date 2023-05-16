-- Check if the 'latest_version' column exists in posts table before adding it back
DO $$
BEGIN
  IF NOT EXISTS(SELECT * FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'latest_version') THEN
    ALTER TABLE posts ADD COLUMN latest_version BIGINT REFERENCES versions(id);
  END IF;
END $$;
