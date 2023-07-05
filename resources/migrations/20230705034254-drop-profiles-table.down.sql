-- recreate the profiles table
CREATE TABLE profiles (
  id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, -- this is the id of the user; if a user is deleted, all info about their profile should also automatically be deleted
  name TEXT NOT NULL,
  public_email VARCHAR(255),
  gravatar_email VARCHAR(255),
  gravatar_id VARCHAR(32),
  location TEXT,
  website TEXT,
  bio TEXT,
  timezone VARCHAR(40),
  notes TEXT
);