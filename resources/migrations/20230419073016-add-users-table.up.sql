CREATE TABLE users (
  id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password TEXT NOT NULL, -- the *hashed* & salted password!
  unconfirmed_email VARCHAR(255) NOT NULL,
  registration_ip VARCHAR(45) NOT NULL,
  admin BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  confirmed_at TIMESTAMP,
  updated_at TIMESTAMP,
  blocked_at TIMESTAMP,
  last_login_at TIMESTAMP
);
