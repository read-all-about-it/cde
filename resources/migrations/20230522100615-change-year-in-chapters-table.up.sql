-- change 'year' column in chapters table to 'pub-year' for clarity
ALTER TABLE chapters RENAME COLUMN year TO pub_year;