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
       authors.common_name AS author_common_name,
       authors.nationality AS author_nationality,
       authors.other_name AS author_other_name,
       authors.gender AS author_gender
FROM titles
JOIN newspapers ON titles.newspaper_table_id = newspapers.id
JOIN authors ON titles.author_id = authors.id
WHERE authors.nationality ILIKE COALESCE(:author_nationality, authors.nationality)
AND newspapers.common_title ILIKE COALESCE(:newspaper_title_text, newspapers.common_title)
AND (
    titles.common_title ILIKE COALESCE(:title_text, titles.common_title)
    OR titles.publication_title ILIKE COALESCE(:title_text, titles.publication_title)
)
AND (
    authors.common_name ILIKE COALESCE(:author_name, authors.common_name)
    OR titles.attributed_author_name ILIKE COALESCE(:author_name, titles.attributed_author_name)
)
ORDER BY titles.common_title ASC
LIMIT :limit
OFFSET :offset


-- :name search-chapters* :? :*
-- :doc searches in chapters for a given text string, filtering by title & author facets, and limiting the results
SELECT chapters.*,
       titles.common_title AS title_common_title,
       titles.author_id AS author_id,
       titles.newspaper_table_id AS newspaper_table_id,
       newspapers.common_title AS newspaper_common_title,
       authors.common_name AS author_common_name
FROM chapters
JOIN titles ON chapters.title_id = titles.id
JOIN newspapers ON titles.newspaper_table_id = newspapers.id
JOIN authors ON titles.author_id = authors.id
WHERE chapters.chapter_text ILIKE COALESCE(:chapter_text, chapters.chapter_text)
ORDER BY chapters.chapter_text ASC
LIMIT :limit
OFFSET :offset


-- :name get-newspaper-by-trove-newspaper-id* :? :*
-- :doc selects newspapers by trove-newspaper-id
SELECT * FROM newspapers
WHERE trove_newspaper_id = :trove_newspaper_id

-- :name get-chapter-by-trove-article-id* :? :*
-- :doc selects chapters which have a given trove-article-id
SELECT * FROM chapters
WHERE trove_article_id = :trove_article_id

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
ORDER BY nationality ASC

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

-- :name get-title-by-id-with-author-newspaper-names* :? :1
-- :doc selects a title by id and join the author and newspaper common names
SELECT titles.*, 
       newspapers.title AS newspaper_title, 
       newspapers.common_title AS newspaper_common_title, 
       authors.common_name AS author_common_name,
       authors.nationality AS author_nationality,
       authors.gender AS author_gender
FROM titles
JOIN newspapers ON titles.newspaper_table_id = newspapers.id
JOIN authors ON titles.author_id = authors.id
WHERE titles.id = :id

-- :name get-chapter-by-id* :? :1
-- :doc selects a chapter by id
SELECT chapters.*,
        titles.common_title AS title_common_title,
        titles.publication_title AS title_publication_title
FROM chapters
JOIN titles ON chapters.title_id = titles.id
WHERE chapters.id = :id

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

-- :name get-all-titles-by-author-id* :? :?
-- :doc selects all titles with a given author_id (including newspaper names for each title)
SELECT titles.*,
       newspapers.title AS newspaper_title,
       newspapers.common_title AS newspaper_common_title
FROM titles
JOIN newspapers ON titles.newspaper_table_id = newspapers.id
WHERE author_id = :author_id

-- :name get-all-titles-by-newspaper-table-id* :? :?
-- :doc selects all titles with a given newspaper_table_id (including author names for each title)
SELECT titles.*,
       authors.common_name AS author_common_name
FROM titles
JOIN authors ON titles.author_id = authors.id
WHERE newspaper_table_id = :newspaper_table_id


-- :name get-terse-newspaper-list* :? :*
-- :doc selects all newspapers, ordered by common_title, returning id, trove_newspaper_id, title, and common_title
SELECT id, trove_newspaper_id, title, common_title
FROM newspapers
ORDER BY common_title ASC

-- :name get-terse-author-list* :? :*
-- :doc selects all authors, ordered by common_name, returning id, common_name, and other_name
SELECT id, common_name, other_name
FROM authors
ORDER BY common_name ASC

-- :name get-terse-title-list* :? :*
-- :doc selects all titles, ordered by publication_title, returning id, publication_title, and common_title
SELECT id, publication_title, common_title
FROM titles
ORDER BY publication_title ASC


-- :name update-title!* :! :1
-- :doc updates an existing title record
UPDATE titles
SET 
    newspaper_table_id = :newspaper_table_id, 
    span_start = :span_start, 
    span_end = :span_end, 
    publication_title = :publication_title, 
    attributed_author_name = :attributed_author_name, 
    common_title = :common_title, 
    author_id = :author_id, 
    author_of = :author_of, 
    additional_info = :additional_info, 
    inscribed_author_nationality = :inscribed_author_nationality, 
    inscribed_author_gender = :inscribed_author_gender, 
    information_source = :information_source, 
    length = :length, 
    trove_source = :trove_source, 
    also_published = :also_published, 
    name_category = :name_category, 
    curated_dataset = :curated_dataset, 
    added_by = :added_by, 
    updated_at = NOW()
WHERE id = :id
RETURNING *

-- :name update-chapter!* :! :1
-- :doc updates an existing chapter record
UPDATE chapters
SET
    chapter_number = :chapter_number, 
    chapter_title = :chapter_title, 
    article_url = :article_url, 
    dow = :dow, 
    pub_day = :pub_day, 
    pub_month = :pub_month, 
    pub_year = :pub_year, 
    final_date = :final_date, 
    page_references = :page_references, 
    page_url = :page_url,
    corrections = :corrections, 
    word_count = :word_count,
    illustrated = :illustrated,
    last_corrected = :last_corrected, 
    page_sequence = :page_sequence, 
    chapter_html = :chapter_html, 
    chapter_text = :chapter_text, 
    text_title = :text_title, 
    export_title = :export_title, 
    updated_at = NOW()
WHERE id = :id
RETURNING *