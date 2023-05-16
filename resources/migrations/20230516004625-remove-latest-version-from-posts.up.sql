-- Check if the 'latest_version' column exists in posts table before dropping it
DO $$
BEGIN
  IF NOT EXISTS(SELECT * FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'latest_version') THEN
    ALTER TABLE posts DROP COLUMN latest_version;
  END IF;
END $$;
