CREATE TABLE chapters (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title_id INTEGER REFERENCES titles(id), -- the title/story that this chapter is a part of
    newspaper_id INTEGER REFERENCES newspapers(id), -- the newspaper that this chapter was published in
    author_id INTEGER REFERENCES authors(id), 
    chapter_number TEXT,
    chapter_title TEXT,
    article_url TEXT, -- link to the nla article record, eg ordinarily something like "http://nla.gov.au/nla.news-article21807793" for the chapter with 21807793 as its id
    dow TEXT, -- eg "Monday", "Tuesday", etc. (this was 'day' in the old database)
    day INTEGER, -- day of the month, eg 3 for the 3rd or 21 for the 21st; was 'date' in the old database
    month INTEGER, -- eg 1 for January, 2 for February, etc.
    year INTEGER, -- eg 1871 for 1871
    final_date DATE, -- eg 1871-01-03 for January 3, 1871
    page_references INTEGER,
    page_url TEXT,
    corrections INTEGER NOT NULL DEFAULT 0, -- number of corrections
    word_count INTEGER, -- number of words in the article
    illustrated BOOLEAN,
    last_corrected DATE, -- eg 2014-01-01 for January 1, 2014
    page_sequence TEXT,
    article_html TEXT, -- note: this was 'article_text' in the old database!
    article_text TEXT, -- note: this didn't exist in the old database (it was just 'article_html')
    text_title TEXT,
    output BOOLEAN DEFAULT FALSE,
    export_title TEXT,
    updated_at TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW(),
    added_by INTEGER REFERENCES users(id)
);
