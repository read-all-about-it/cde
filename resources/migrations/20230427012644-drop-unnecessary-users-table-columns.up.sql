-- Check if the columns exist before dropping them
DO $$
BEGIN
  IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'unconfirmed_email') THEN
    ALTER TABLE users DROP COLUMN unconfirmed_email;
  END IF;

  IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'registration_ip') THEN
    ALTER TABLE users DROP COLUMN registration_ip;
  END IF;

  IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'confirmed_at') THEN
    ALTER TABLE users DROP COLUMN confirmed_at;
  END IF;
END $$;
