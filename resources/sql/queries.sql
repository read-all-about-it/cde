-- :name create-user!* :! :1
-- :doc creates a new user record given a map containing an email
INSERT INTO users
(email)
VALUES (:email)
RETURNING id

-- :name get-user-from-email* :? :1
-- :doc Selects a user given an email address
SELECT * FROM users
WHERE email = :email

-- :name search-titles* :? :*
-- :doc Searches for titles with optional filters on title text, newspaper, author name, and nationality.
--      Uses ILIKE for substring matching. Each filter is optional - when NULL, that filter is skipped.
--      Returns titles joined with newspaper and author information.
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
WHERE
    -- Author nationality filter (optional): skip if param is NULL
    (:author_nationality::text IS NULL OR authors.nationality ILIKE '%' || :author_nationality || '%')
AND
    -- Newspaper title filter (optional): search in both title and common_title
    (:newspaper_title_text::text IS NULL OR (
        COALESCE(newspapers.title, '') ILIKE '%' || :newspaper_title_text || '%'
        OR COALESCE(newspapers.common_title, '') ILIKE '%' || :newspaper_title_text || '%'
    ))
AND
    -- Title text filter (optional): search in both common_title and publication_title
    (:title_text::text IS NULL OR (
        COALESCE(titles.common_title, '') ILIKE '%' || :title_text || '%'
        OR COALESCE(titles.publication_title, '') ILIKE '%' || :title_text || '%'
    ))
AND
    -- Author name filter (optional): search in author common_name and attributed_author_name
    (:author_name::text IS NULL OR (
        COALESCE(authors.common_name, '') ILIKE '%' || :author_name || '%'
        OR COALESCE(authors.other_name, '') ILIKE '%' || :author_name || '%'
        OR COALESCE(titles.attributed_author_name, '') ILIKE '%' || :author_name || '%'
    ))
ORDER BY titles.common_title ASC NULLS LAST, titles.publication_title ASC
LIMIT :limit
OFFSET :offset



-- :name search-chapters* :? :*
-- :doc Searches chapters with optional filters on chapter text, title, newspaper, author name, and nationality.
--      Uses ILIKE for substring matching. Each filter is optional - when NULL, that filter is skipped.
--      Returns chapters joined with title, newspaper, and author information.
SELECT chapters.*,
       titles.common_title AS title_common_title,
       titles.publication_title AS title_publication_title,
       titles.author_id AS author_id,
       titles.newspaper_table_id AS newspaper_table_id,
       newspapers.common_title AS newspaper_common_title,
       newspapers.title AS newspaper_title,
       authors.common_name AS author_common_name
FROM chapters
JOIN titles ON chapters.title_id = titles.id
JOIN newspapers ON titles.newspaper_table_id = newspapers.id
JOIN authors ON titles.author_id = authors.id
WHERE
    -- Chapter text filter (optional): search within chapter content
    (:chapter_text::text IS NULL OR COALESCE(chapters.chapter_text, '') ILIKE '%' || :chapter_text || '%')
AND
    -- Title text filter (optional): search in title names
    (:title_text::text IS NULL OR (
        COALESCE(titles.common_title, '') ILIKE '%' || :title_text || '%'
        OR COALESCE(titles.publication_title, '') ILIKE '%' || :title_text || '%'
    ))
AND
    -- Newspaper title filter (optional): search in newspaper names
    (:newspaper_title_text::text IS NULL OR (
        COALESCE(newspapers.title, '') ILIKE '%' || :newspaper_title_text || '%'
        OR COALESCE(newspapers.common_title, '') ILIKE '%' || :newspaper_title_text || '%'
    ))
AND
    -- Author nationality filter (optional)
    (:author_nationality::text IS NULL OR authors.nationality ILIKE '%' || :author_nationality || '%')
AND
    -- Author name filter (optional): search in author names
    (:author_name::text IS NULL OR (
        COALESCE(authors.common_name, '') ILIKE '%' || :author_name || '%'
        OR COALESCE(authors.other_name, '') ILIKE '%' || :author_name || '%'
    ))
ORDER BY chapters.final_date ASC NULLS LAST, chapters.id ASC
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
-- :doc selects a chapter by id with related title, author, and newspaper info
SELECT chapters.*,
        titles.common_title AS title_common_title,
        titles.publication_title AS title_publication_title,
        titles.author_id AS author_id,
        titles.newspaper_table_id AS newspaper_table_id,
        authors.common_name AS author_common_name,
        newspapers.common_title AS newspaper_common_title
FROM chapters
JOIN titles ON chapters.title_id = titles.id
JOIN authors ON titles.author_id = authors.id
JOIN newspapers ON titles.newspaper_table_id = newspapers.id
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
ORDER BY final_date ASC

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
    page_sequence = :page_sequence,
    chapter_html = :chapter_html,
    chapter_text = :chapter_text,
    text_title = :text_title,
    export_title = :export_title,
    updated_at = NOW()
WHERE id = :id
RETURNING *

-- :name update-author!* :! :1
-- :doc updates an existing author record
UPDATE authors
SET
    common_name = :common_name,
    other_name = :other_name,
    gender = :gender,
    nationality = :nationality,
    nationality_details = :nationality_details,
    author_details = :author_details,
    updated_at = NOW()
WHERE id = :id
RETURNING *

-- :name update-newspaper!* :! :1
-- :doc updates an existing newspaper record
UPDATE newspapers
SET
    title = :title,
    common_title = :common_title,
    location = :location,
    start_year = :start_year,
    end_year = :end_year,
    details = :details,
    newspaper_type = :newspaper_type,
    colony_state = :colony_state,
    start_date = :start_date,
    end_date = :end_date,
    issn = :issn,
    updated_at = NOW()
WHERE id = :id
RETURNING *


-- :name get-newspapers* :? :*
-- :doc gets a list of all newspapers with a limit and offset
SELECT * FROM newspapers
LIMIT :limit
OFFSET :offset


-- :name get-authors* :? :*
-- :doc gets a list of all authors with a limit and offset
SELECT * FROM authors
LIMIT :limit
OFFSET :offset

-- :name get-titles* :? :*
-- :doc gets a list of all titles with a limit and offset
SELECT * FROM titles
LIMIT :limit
OFFSET :offset

-- :name get-chapters* :? :*
-- :doc gets a list of all chapters with a limit and offset
SELECT * FROM chapters
LIMIT :limit
OFFSET :offset
