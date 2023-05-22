-- change 'day' column in chapters table to 'pub-day' for clarity
ALTER TABLE chapters RENAME COLUMN day TO pub_day;