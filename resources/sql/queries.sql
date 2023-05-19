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

-- :name get-newspaper-by-trove-newspaper-id* :? :1
-- :doc selects a newspaper by trove-newspaper-id
SELECT * FROM newspapers
WHERE trove_newspaper_id = :trove_newspaper_id

-- :name create-newspaper!* :! :n
-- :doc creates a new newspaper record
INSERT INTO newspapers
(trove_newspaper_id, title, common_title, location, start_year, end_year, details, newspaper_type, colony_state, start_date, end_date, issn, added_by)
VALUES (:trove_newspaper_id, :title, :common_title, :location, :start_year, :end_year, :details, :newspaper_type, :colony_state, :start_date, :end_date, :issn, :added_by)
RETURNING id

-- :name get-author-by-id* :? :1
-- :doc selects an author by id
SELECT * FROM authors
WHERE id = :id

-- :name get-unique-author-genders* :? :*
-- :doc get all unique values in the 'gender' column of the authors table
SELECT DISTINCT gender 
FROM authors 
WHERE gender IS NOT NULL

-- :name get-unique-author-nationalities* :? :*
-- :doc get all unique values in the 'nationality' column of the authors table
SELECT DISTINCT nationality
FROM authors
WHERE nationality IS NOT NULL

-- :name create-author!* :! :n
-- :doc creates a new author record
INSERT INTO authors
(common_name, other_name, gender, nationality, nationality_details, author_details, added_by)
VALUES (:common_name, :other_name, :gender, :nationality, :nationality_details, :author_details, :added_by)
RETURNING id

-- :name create-title!* :! :n
-- :doc creates a new title record
INSERT INTO titles
(newspaper_table_id, span_start, span_end, publication_title, attributed_author_name, common_title, author_id, author_of, additional_info, inscribed_author_nationality, inscribed_author_gender, information_source, length, trove_source, also_published, name_category, curated_dataset, added_by)
VALUES (:newspaper_table_id, :span_start, :span_end, :publication_title, :attributed_author_name, :common_title, :author_id, :author_of, :additional_info, :inscribed_author_nationality, :inscribed_author_gender, :information_source, :length, :trove_source, :also_published, :name_category, :curated_dataset, :added_by)

-- :name create-chapter!* :! :n
-- :doc creates a new chapter record
INSERT INTO chapters
(title_id, newspaper_table_id, author_id, chapter_number, chapter_title, article_url, dow, day, month, year, final_date, page_references, page_url, word_count, illustrated, page_sequence, chapter_html, chapter_text, text_title, export_title, added_by)
VALUES (:title_id, :newspaper_table_id, :author_id, :chapter_number, :chapter_title, :article_url, :dow, :day, :month, :year, :final_date, :page_references, :page_url, :word_count, :illustrated, :page_sequence, :chapter_html, :chapter_text, :text_title, :export_title, :added_by)