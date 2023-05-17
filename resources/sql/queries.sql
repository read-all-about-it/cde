-- :name create-user!* :! :n
-- :doc creates a new user record and a matching profile record
WITH new_user AS (
  INSERT INTO users
  (username, email, password)
  VALUES (:username, :email, :password)
  RETURNING id
)
INSERT INTO profiles
(user_id, name)
VALUES ((SELECT id FROM new_user), :username)

-- :name get-user-for-auth-by-username* :? :1
-- :doc selects a user for authentication (using username)
SELECT * FROM users
WHERE username = :username

-- :name get-user-for-auth-by-email* :? :1
-- :doc selects a user for authentication (using email)
SELECT * FROM users
WHERE email = :email

-- :name get-user-profile* :? :1
-- :doc selects a user profile (using user-id)
SELECT * FROM profiles
WHERE user_id = :id


-- :name search-titles* :? :*
-- :doc searches for titles based on the given query, limit, and offset
SELECT t.* FROM titles t
LEFT JOIN authors a ON t.author_id = a.id
LEFT JOIN newspapers n ON t.newspaper_id = n.id
WHERE (:length::integer IS NULL OR t.length = :length)
AND (:nationality::text IS NULL OR a.nationality ILIKE :nationality)
AND (:author::text IS NULL OR a.common_name ILIKE :author)
AND (:newspaper_title::text IS NULL OR n.common_title ILIKE :newspaper_title)
AND (:common_title::text IS NULL OR t.common_title ILIKE :common_title)
ORDER BY t.title ASC
LIMIT :limit
OFFSET :offset

-- :name create-newspaper!* :! :n
-- :doc creates a new newspaper record
INSERT INTO newspapers
(newspaper_id, title, common_title, location, start_year, end_year, details, newspaper_type, colony_state, start_date, end_date, issn)
VALUES (:newspaper_id, :title, :common_title, :location, :start_year, :end_year, :details, :newspaper_type, :colony_state, :start_date, :end_date, :issn)