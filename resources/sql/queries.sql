-- :name create-user!* :! :n
-- :doc creates a new user record
INSERT INTO users
(username, email, password)
VALUES (:username, :email, :password)

-- :name get-user-for-auth-by-username* :? :1
-- :doc selects a user for authentication (using username)
SELECT * FROM USERS
WHERE username = :username

-- :name get-user-for-auth-by-email* :? :1
-- :doc selects a user for authentication (using email)
SELECT * FROM USERS
WHERE email = :email

