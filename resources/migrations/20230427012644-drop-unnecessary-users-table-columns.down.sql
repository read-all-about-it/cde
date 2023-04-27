-- Check if the columns exist before adding them back
DO $$
BEGIN
  IF NOT EXISTS(SELECT * FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'unconfirmed_email') THEN
    ALTER TABLE users ADD COLUMN unconfirmed_email VARCHAR(255) NOT NULL;
  END IF;

  IF NOT EXISTS(SELECT * FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'registration_ip') THEN
    ALTER TABLE users ADD COLUMN registration_ip VARCHAR(45) NOT NULL;
  END IF;

  IF NOT EXISTS(SELECT * FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'confirmed_at') THEN
    ALTER TABLE users ADD COLUMN confirmed_at TIMESTAMP;
  END IF;
END $$;
