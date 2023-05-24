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
SELECT titles.*, 
       newspapers.title AS newspaper_title, 
       newspapers.common_title AS newspaper_common_title, 
       authors.common_name AS author_common_name
FROM titles
JOIN newspapers ON titles.newspaper_table_id = newspapers.id
JOIN authors ON titles.author_id = authors.id
WHERE titles.common_title ILIKE :common_title
ORDER BY titles.common_title ASC
LIMIT :limit
OFFSET :offset


-- :name get-newspaper-by-trove-newspaper-id* :? :1
-- :doc selects a newspaper by trove-newspaper-id
SELECT * FROM newspapers
WHERE trove_newspaper_id = :trove_newspaper_id

-- :name create-newspaper!* :! :1
-- :doc creates a new newspaper record
INSERT INTO newspapers
(trove_newspaper_id, title, common_title, location, start_year, end_year, details, newspaper_type, colony_state, start_date, end_date, issn, added_by)
VALUES (:trove_newspaper_id, :title, :common_title, :location, :start_year, :end_year, :details, :newspaper_type, :colony_state, :start_date, :end_date, :issn, :added_by)
RETURNING id


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

-- :name create-author!* :! :1
-- :doc creates a new author record
INSERT INTO authors
(common_name, other_name, gender, nationality, nationality_details, author_details, added_by)
VALUES (:common_name, :other_name, :gender, :nationality, :nationality_details, :author_details, :added_by)
RETURNING id

-- :name create-title!* :! :1
-- :doc creates a new title record
INSERT INTO titles
(newspaper_table_id, span_start, span_end, publication_title, attributed_author_name, common_title, author_id, author_of, additional_info, inscribed_author_nationality, inscribed_author_gender, information_source, length, trove_source, also_published, name_category, curated_dataset, added_by)
VALUES (:newspaper_table_id, :span_start, :span_end, :publication_title, :attributed_author_name, :common_title, :author_id, :author_of, :additional_info, :inscribed_author_nationality, :inscribed_author_gender, :information_source, :length, :trove_source, :also_published, :name_category, :curated_dataset, :added_by)
RETURNING id

-- :name create-chapter!* :! :1
-- :doc creates a new chapter record
INSERT INTO chapters
(title_id, trove_article_id, chapter_number, chapter_title, article_url, dow, pub_day, pub_month, pub_year, final_date, page_references, page_url, word_count, illustrated, page_sequence, chapter_html, chapter_text, text_title, export_title, added_by)
VALUES (:title_id, :trove_article_id, :chapter_number, :chapter_title, :article_url, :dow, :pub_day, :pub_month, :pub_year, :final_date, :page_references, :page_url, :word_count, :illustrated, :page_sequence, :chapter_html, :chapter_text, :text_title, :export_title, :added_by)
RETURNING id


-- :name get-newspaper-by-id* :? :1
-- :doc selects a newspaper by id
SELECT * FROM newspapers
WHERE id = :id

-- :name get-author-by-id* :? :1
-- :doc selects an author by id
SELECT * FROM authors
WHERE id = :id

-- :name get-title-by-id* :? :1
-- :doc selects a title by id
SELECT * FROM titles
WHERE id = :id

-- :name get-chapter-by-id* :? :1
-- :doc selects a chapter by id
SELECT * FROM chapters
WHERE id = :id

-- :name count-newspapers* :? :1
-- :doc counts the number of newspapers in the database
SELECT COUNT(*) FROM newspapers

-- :name count-authors* :? :1
-- :doc counts the number of authors in the database
SELECT COUNT(*) FROM authors

-- :name count-titles* :? :1
-- :doc counts the number of titles in the database
SELECT COUNT(*) FROM titles

-- :name count-chapters* :? :1
-- :doc counts the number of chapters in the database
SELECT COUNT(*) FROM chapters

-- :name get-all-chapters-in-title* :? :?
-- :doc selects all chapters with a given title_id
SELECT * FROM chapters
WHERE title_id = :title_id