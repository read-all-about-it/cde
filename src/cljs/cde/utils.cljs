(ns cde.utils
  "Frontend utility functions and field metadata definitions.

  Provides two categories of functionality:

  1. **API Utilities**: Helper functions for constructing API endpoints
     and formatting data for display.

  2. **Field Metadata**: Comprehensive definitions for how to display,
     edit, and transform fields for each entity type (title, author,
     chapter, newspaper). This metadata drives the dynamic form and
     table components throughout the application.

  Key functions:
  - [[endpoint]] - Constructs API endpoint URLs
  - [[details->metadata]] - Transforms API responses for UI display
  - [[key->help]], [[key->title]], [[key->placeholder]] - Field metadata lookup

  See also: [[cde.components.metadata]], [[cde.components.forms]]."
  (:require
   [clojure.string :as str]))

;;;; API Helpers

(def api-url
  "Base URL for API endpoints.

  All API requests are prefixed with this path.
  TODO: Move to config/env file."
  "/api/v1")

(defn endpoint
  "Constructs an API endpoint URL by joining path segments.

  Concatenates the base [[api-url]] with provided path segments,
  separated by forward slashes.

  Arguments:
  - `params` - variadic path segments to append

  Returns: string URL path.

  Example: `(endpoint \"titles\" 123)` => `\"/api/v1/titles/123\"`"
  [& params]
  (str/join "/" (cons api-url params)))

;;;; Display Formatting

(defn length-integer->human-string
  "Converts a title length code to a human-readable description.

  Title length codes indicate the type/size of the serialised work:
  - `0` - Serialised across multiple editions
  - `1` - Short story in single edition
  - `8` - Long story (10,000+ words) in single edition

  Arguments:
  - `length` - integer length code from title record

  Returns: human-readable string description."
  [length]
  (cond
    (= length 0) "Serialised Title"
    (= length 1) "Short Single Edition"
    (= length 8) "10,000+ Words (Single Edition)"
    :else "Unknown"))

(defn pretty-number
  "Formats a number in concise, human-readable form for display.

  Applies appropriate formatting based on magnitude:
  - Under 1,000: displayed as-is
  - 1,000-9,999: comma-separated (e.g., \"1,234\")
  - 10,000-999,999: abbreviated with K suffix (e.g., \"10K\", \"45.2K\")
  - 1,000,000+: abbreviated with M suffix (e.g., \"1M\", \"2.5M\")

  Arguments:
  - `n` - integer to format

  Returns: formatted string representation."
  [n]
  (cond
    (< n 1000) (str n)
    (< n 10000) (str (int (/ n 1000)) "," (mod n 1000))
    (< n 1000000) (let [k (float (/ n 1000))]
                    (if (zero? (mod n 1000))
                      (str (int k) "K")
                      (str (.toFixed k 1) "K")))
    :else (let [m (float (/ n 1000000))]
            (if (zero? (mod n 1000000))
              (str (int m) "M")
              (str (.toFixed m 1) "M")))))

;;;; Field Metadata Definitions
;;
;; The following parameter vectors define metadata for each entity type.
;; Each entry is a map with keys controlling display, editing, and transformation:
;;
;; - `:default-key` - keyword matching the API response field
;; - `:show-to-user?` - boolean, whether to display to users
;; - `:editable?` - boolean, whether users can modify via edit forms
;; - `:title` - human-friendly label for the field
;; - `:placeholder` - hint text for form inputs
;; - `:keep?` - boolean, whether to show field even when value is nil
;; - `:display-default` - fallback value when actual value is nil
;; - `:help` - detailed explanation text for tooltips
;; - `:always-show?` - boolean, show in collapsed/summary views
;; - `:translation` - fn to transform value for display (or nil)
;; - `:show-in-horizontal?` - boolean, include in horizontal table layouts
;; - `:link-to` - fn taking record, returns URL string (or nil)

(def ^:private title-parameters
  "Field metadata for title (serialised fiction work) records."
  [{:default-key :common_title
    :placeholder "Common Title"
    :show-to-user? true
    :editable? true
    :title "Common Title"
    :keep? true
    :display-default ""
    :help "This is the title that the story is 'commonly known as', even if some newspapers published it under a different title."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to #(str "/#/title/" (:id %))}
   {:default-key :publication_title
    :placeholder "Publication Title"
    :show-to-user? true
    :editable? true
    :title "Publication Title"
    :keep? true
    :display-default ""
    :help "This is the title that the story was published under in this particular instance."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to #(str "/#/title/" (:id %))}
   {:default-key :author_common_name
    :show-to-user? true
    :editable? false
    :title "Author"
    :keep? true
    :display-default ""
    :help "This is the common name of the author believed to have written the story (not necessarily the name attributed in the publication itself)."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to #(str "/#/author/" (:author_id %))}
   {:default-key :span_start
    :show-to-user? true
    :editable? true
    :title "Start Date"
    :keep? true
    :display-default ""
    :help "This is the date the first chapter of the story was published in the newspaper."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to nil}
   {:default-key :span_end
    :show-to-user? true
    :editable? true
    :title "End Date"
    :keep? true
    :display-default ""
    :help "This is the date the last chapter of the story was published in the newspaper."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to nil}
   {:default-key :length
    :show-to-user? true
    :editable? true
    :title "Length"
    :keep? false
    :display-default ""
    :help "This is the length of the title (whether a short story in a single edition, or a story serialised over multiple editions)."
    :always-show? false
    :translation length-integer->human-string
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :inscribed_author_nationality
    :placeholder "eg 'British'"
    :show-to-user? true
    :editable? true
    :title "Inscribed Author Nationality"
    :keep? false
    :display-default ""
    :help "This is the nationality of the author as it appears in the publication itself (ie, 'a new story by an Australian author')."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :information_source
    :placeholder "eg 'Wikipedia', 'Austlit'"
    :show-to-user? true
    :editable? true
    :title "Information Source"
    :keep? false
    :display-default ""
    :help "This is the source that provides additional information about the title."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :newspaper_common_title
    :show-to-user? false
    :editable? false
    :title "Published In"
    :keep? false
    :display-default ""
    :help "This is the 'common' title of the newspaper that published the title was published in."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to #(str "/#/newspaper/" (:newspaper_table_id %))}
   {:default-key :newspaper_title
    :show-to-user? true
    :editable? false
    :title "Newspaper Title"
    :keep? false
    :display-default ""
    :help "This is the 'long' title of the newspaper that published the title was published in."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to #(str "/#/newspaper/" (:newspaper_table_id %))}
   {:default-key :inscribed_author_gender
    :placeholder "eg 'female'"
    :show-to-user? true
    :editable? true
    :title "Inscribed Author Gender"
    :keep? false
    :display-default "None"
    :help "This is the author gender as it appears in the publication itself (ie, 'a new story by a woman from Adelaide')."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :attributed_author_name
    :placeholder "eg 'Smith, Bill'"
    :show-to-user? true
    :editable? true
    :title "Attributed Author Name"
    :keep? false
    :display-default ""
    :help "This is the name of the author as it appears in the publication itself (ie, 'Smith, John' for 'a new story by John Smith')."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :name_category
    :placeholder "eg Pseudonym, initials, etc"
    :show-to-user? true
    :editable? true
    :title "Attribution Type"
    :keep? false
    :display-default ""
    :help "This is how the author is attributed in the publication itself (ie, by name, initials, pseudonym, etc.)"
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :curated_dataset
    :show-to-user? false
    :editable? false
    :title "Curated Dataset"
    :keep? false
    :display-default ""
    :help "This is whether the title is part of the curated dataset."
    :always-show? false
    :translation #(if % "Yes" "No")
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :author_of
    :placeholder "'Mr Hogarth's Will', 'Hugh Lindsay's Guest'"
    :show-to-user? true
    :editable? true
    :title "Attributed Author of"
    :keep? false
    :display-default ""
    :help "Any other works that are attributed to the author in the publication itself (ie, 'from the author of A New Othello')."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :also_published
    :placeholder "Other (external) sources where this story was published."
    :show-to-user? true
    :editable? true
    :title "Also Published In"
    :keep? false
    :display-default ""
    :help "Any other external sources that this title was also published in, whether old or new."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :created_at
    :show-to-user? false
    :editable? false
    :title "Creation Date"
    :keep? false
    :display-default ""
    :help "This is the date the title was added to the database."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :updated_at
    :show-to-user? false
    :editable? false
    :title "Last Updated"
    :keep? false
    :display-default ""
    :help "This is the date the metadata for this title was last updated by a user."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :added_by
    :show-to-user? false
    :editable? false
    :title "Added By"
    :keep? false
    :display-default ""
    :help "This is the id of the user who added this title to the database."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :trove_source
    :show-to-user? false
    :editable? false
    :title "Trove Source"
    :keep? false
    :display-default ""
    :help "TODO: WORK OUT WHAT THIS ACTUALLY IS!"
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :additional_info
    :placeholder "Possible additional information about this story."
    :show-to-user? true
    :editable? true
    :title "Additional Information"
    :keep? false
    :display-default ""
    :help "Any additional information about the title that is not covered by the other fields. For example: copyright information, 'was adapted as a motion picture', republished, etc."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :id
    :show-to-user? false
    :editable? false
    :title "ID"
    :keep? false
    :display-default ""
    :help "This is the unique ID of the title in the database."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :newspaper_table_id
    :show-to-user? false
    :editable? true
    :title "Newspaper ID"
    :keep? false
    :display-default ""
    :help "This is the unique ID (used in the database) of the newspaper that published the title."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :author_id
    :show-to-user? false
    :editable? true
    :title "Author ID"
    :keep? false
    :display-default ""
    :help "This is the unique ID (used in the database) of the author of the title."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}])

(def ^:private author-parameters
  "Field metadata for author records."
  [{:default-key :common_name
    :show-to-user? true
    :title "Author Name"
    :keep? true
    :display-default ""
    :help "The name of the author."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to #(str "/#/author/" (:id %))}
   {:default-key :other_name
    :show-to-user? true
    :title "Other Attributed Names"
    :keep? true
    :display-default ""
    :help "Other names that the author is known by."
    :always-show? true
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :gender
    :show-to-user? true
    :title "Gender"
    :keep? true
    :display-default ""
    :help "The gender of the author."
    :always-show? true
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :nationality
    :show-to-user? true
    :title "Nationality"
    :keep? true
    :display-default ""
    :help "The nationality of the author."
    :always-show? true
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :nationality_details
    :show-to-user? true
    :title "Nationality Details"
    :keep? true
    :display-default ""
    :help "Extra information about the nationality of the author."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :author_details
    :show-to-user? true
    :title "Source of Author Details"
    :keep? true
    :display-default ""
    :help "Sources used to provide details about the author."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :id
    :show-to-user? false
    :title "ID"
    :keep? false
    :display-default ""
    :help "This is the unique ID of the author in the database."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :created_at
    :show-to-user? false
    :title "Creation Date"
    :keep? false
    :display-default ""
    :help "The date the author record was added to the database."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :updated_at
    :show-to-user? false
    :title "Last Updated"
    :keep? false
    :display-default ""
    :help "This is the date the metadata for this author was last updated by a user."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :added_by
    :show-to-user? false
    :title "Added By"
    :keep? false
    :display-default ""
    :help "This is the id of the user who added this author record to the database."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}])

(def ^:private chapter-parameters
  "Field metadata for chapter (story instalment) records."
  [{:default-key :chapter_number
    :show-to-user? true
    :title "Chapter Number"
    :placeholder "XII (or similar)"
    :keep? true
    :display-default ""
    :help "The chapter number of the story as it appears in the newspaper. This is usually a roman numeral (eg 'XII')."
    :always-show? false
    :translation nil
    :show-in-horizontal? true
    :link-to #(str "#/chapter/" (:id %))}
   {:default-key :chapter_title
    :show-to-user? true
    :title "Chapter Title"
    :placeholder "The Untitled Chapter"
    :keep? true
    :display-default "Unnamed Chapter"
    :help "The title of the chapter as it appears in the newspaper."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to #(str "#/chapter/" (:id %))}
   {:default-key :title_id
    :show-to-user? true
    :title "Chapter In"
    :keep? true
    :display-default ""
    :help "The title that the story in which the chapter appears is 'commonly known as', even if some newspapers published it under a different title."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :value-fn (fn [details]
                (or (not-empty (:title_common_title details))
                    (not-empty (:title_publication_title details))
                    "Unknown Title"))
    :link-to (fn [details]
               (when (:title_id details)
                 (str "/#/title/" (:title_id details))))}
   {:default-key :author_common_name
    :show-to-user? true
    :title "Author"
    :keep? true
    :display-default ""
    :help "This is the common name of the author believed to have written the story that this chapter is a part of (not necessarily the name attributed in the publication itself)."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to (fn [details]
               (if (:author_id details)
                 (str "/#/author/" (:author_id details))
                 nil))}
   {:default-key :newspaper_common_title
    :show-to-user? true
    :title "Published In"
    :keep? false
    :display-default ""
    :help "This is the 'common' title of the newspaper that published the title was published in."
    :always-show? true
    :translation nil
    :show-in-horizontal? false
    :link-to (fn [details]
               (if (:newspaper_table_id details)
                 (str "/#/newspaper/" (:newspaper_table_id details))
                 nil))}
   {:default-key :final_date
    :show-to-user? true
    :title "Publication Date"
    :placeholder "YYYY-MM-DD"
    :keep? true
    :display-default ""
    :help "The date that the chapter was published in the newspaper. (This is usually the date of the newspaper issue.)"
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to nil}
   {:default-key :word_count
    :show-to-user? true
    :title "Word Count"
    :keep? true
    :display-default ""
    :help "The number of words in the chapter."
    :always-show? false
    :translation nil
    :show-in-horizontal? true
    :link-to nil}
   {:default-key :illustrated
    :show-to-user? true
    :title "Illustrated"
    :keep? false
    :display-default ""
    :help "Whether the chapter was published with an accompanying illustration in the newspaper."
    :always-show? false
    :translation #(if % "Yes" "No")
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :trove_article_id
    :show-to-user? true
    :title "Trove Article ID"
    :keep? false
    :display-default ""
    :help "The ID of this chapter, as it appears as an article in Trove."
    :always-show? false
    :translation #(str %)
    :show-in-horizontal? false
    :link-to #(str "https://trove.nla.gov.au/newspaper/article/" (:trove_article_id %))}
   {:default-key :output
    :show-to-user? false
    :title "Output"
    :keep? false
    :display-default ""
    :help "Whether the text of the chapter has been output to a file."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :export_title
    :show-to-user? false
    :title "Export Title"
    :keep? false
    :display-default ""
    :help "The title of the chapter as it appears in an export file. (NOTE: MAYBE??? TODO: CLARIFY THIS!)"
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :page_references
    :show-to-user? false
    :title "Page References"
    :keep? false
    :display-default ""
    :help "The page of the newspaper that the chapter appears on."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :page_sequence
    :show-to-user? false
    :title "Page Sequence"
    :keep? false
    :display-default ""
    :help "The sequence of the page(s) of the newspaper that the chapter appears on."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :text_title
    :show-to-user? false
    :title "Text Title"
    :keep? false
    :display-default ""
    :help "A mysterious string from the old database. TODO: CLARIFY THIS!"
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :page_url
    :show-to-user? false
    :title "Page URL"
    :keep? false
    :display-default ""
    :help "A link to view the first page of the newspaper that the chapter appears on in the Trove web interface."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to #(:page_url %)}
   {:default-key :article_url
    :show-to-user? false
    :title "Article URL"
    :keep? false
    :display-default ""
    :help "The Trove URL of the 'article' which is the chapter."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to #(:article_url %)}
   {:default-key :dow
    :show-to-user? false
    :title "Day of Week"
    :keep? false
    :display-default ""
    :help "The day of the week the chapter was published in the newspaper."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :pub_year
    :show-to-user? false
    :title "Publication Year"
    :keep? false
    :display-default ""
    :help "The year the chapter was published in the newspaper."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :pub_month
    :show-to-user? false
    :title "Publication Month"
    :keep? false
    :display-default ""
    :help "The month the chapter was published in the newspaper."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :pub_day
    :show-to-user? false
    :title "Publication Day"
    :keep? false
    :display-default ""
    :help "The day the chapter was published in the newspaper."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :corrections
    :show-to-user? false
    :title "Corrections"
    :keep? false
    :display-default ""
    :help "The number of corrections that have been made to the chapter on Trove."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :chapter_html
    :show-to-user? false
    :title "Chapter HTML"
    :keep? false
    :display-default ""
    :help "The HTML of the chapter as it appears on Trove."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :chapter_text
    :show-to-user? false
    :title "Chapter Text"
    :keep? false
    :display-default ""
    :help "The text of the chapter (generated from the Trove content)."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :author_id
    :show-to-user? false
    :title "Author ID"
    :keep? false
    :display-default ""
    :help "The unique ID (used in the database) of the author of the title."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :title_id
    :show-to-user? false
    :title "Title ID"
    :keep? false
    :display-default ""
    :help "The unique ID (used in the database) of the title that contains this story."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :id
    :show-to-user? false
    :title "ID"
    :keep? false
    :display-default ""
    :help "This is the unique ID (used in the database) of the chapter."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :created_at
    :show-to-user? false
    :title "Creation Date"
    :keep? false
    :display-default ""
    :help "The date the author record was added to the database."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :updated_at
    :show-to-user? false
    :title "Last Updated"
    :keep? false
    :display-default ""
    :help "The date the metadata for this chapter was last updated by a user."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :last_corrected
    :show-to-user? false
    :title "Last Corrected"
    :keep? false
    :display-default ""
    :help "The date that the text of this chapter was last corrected (on Trove) by a user."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :added_by
    :show-to-user? false
    :title "Added By"
    :keep? false
    :display-default ""
    :help "This is the id of the user who added this chapter record to the database."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}])

(def ^:private newspaper-parameters
  "Field metadata for newspaper publication records."
  [{:default-key :title
    :show-to-user? true
    :title "Newspaper Title"
    :keep? false
    :display-default ""
    :help "This is the 'long' (complete) title of the newspaper."
    :always-show? true
    :translation nil
    :show-in-horizontal? false
    :link-to #(str "/#/newspaper/" (:id %))}
   {:default-key :common_title
    :show-to-user? false
    :title "Common Title"
    :keep? false
    :display-default ""
    :help "This is the 'common' title of the newspaper."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to #(str "/#/newspaper/" (:id %))}
   {:default-key :start_date
    :show-to-user? true
    :title "Start Date"
    :keep? true
    :display-default ""
    :help "The first date that an issue of this newspaper was published."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to nil}
   {:default-key :end_date
    :show-to-user? true
    :title "End Date"
    :keep? true
    :display-default ""
    :help "The last date that an issue of this newspaper was published."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to nil}
   {:default-key :newspaper_type
    :placeholder "eg 'metropolitan'"
    :show-to-user? true
    :title "Newspaper Type"
    :keep? true
    :display-default ""
    :help "The type of newspaper (eg 'metropolitan', etc)."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :location
    :placeholder "eg 'Brisbane'"
    :show-to-user? true
    :title "Location"
    :keep? true
    :display-default ""
    :help "The place that the newspaper was published in."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :colony_state
    :placeholder "eg 'New South Wales'"
    :show-to-user? true
    :title "Colony/State"
    :keep? true
    :display-default ""
    :help "The colony or state that the newspaper was published in."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :issn
    :show-to-user? true
    :title "ISSN"
    :keep? true
    :display-default ""
    :help "The ISSN of the newspaper."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :details
    :placeholder "Any other details of note about the newspaper ..."
    :show-to-user? true
    :title "Details"
    :keep? true
    :display-default ""
    :help "Other details about the newspaper."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :trove_newspaper_id
    :show-to-user? false
    :title "Trove Newspaper ID"
    :keep? false
    :display-default ""
    :help "The unique ID (used in Trove) of the newspaper."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :start_year
    :show-to-user? false
    :title "Start Year"
    :keep? false
    :display-default ""
    :help "The first year that this newspaper published an issue."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :end_year
    :show-to-user? false
    :title "End Year"
    :keep? false
    :display-default ""
    :help "The last year that this newspaper published an issue."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :id
    :show-to-user? false
    :title "ID"
    :keep? false
    :display-default ""
    :help "This is the unique ID (used in the database) of the newspaper"
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :created_at
    :show-to-user? false
    :title "Creation Date"
    :keep? false
    :display-default ""
    :help "The date the newspaper record was added to the database."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :updated_at
    :show-to-user? false
    :title "Last Updated"
    :keep? false
    :display-default ""
    :help "The date the metadata for this newspaper was last updated by a user."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :added_by
    :show-to-user? false
    :title "Added By"
    :keep? false
    :display-default ""
    :help "This is the id of the user who added this newspaper record to the database."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}])

;;;; Metadata Transformation Functions

(defn- ^:no-doc create-structured-param
  "Transforms a single field value using its parameter metadata.

  Takes a parameter definition map and a record's details, producing
  a structured map suitable for the metadata-table component with
  translated values and optional links.

  Arguments:
  - `parameter-map` - field metadata from *-parameters vectors
  - `details` - complete record map from API

  Supports `:value-fn` in parameter-map for custom value computation from
  the full details map, with fallback to `:default-key` lookup.

  Returns: map with `:title`, `:help`, `:value`, `:always-show?`,
           `:show-in-horizontal?` keys, or empty map if field should be hidden."
  [parameter-map details]
  (let [;; Compute raw value: use :value-fn if present, otherwise lookup by :default-key
        compute-raw-value (fn []
                            (if-let [value-fn (:value-fn parameter-map)]
                              (value-fn details)
                              (get details (:default-key parameter-map))))
        raw-value (compute-raw-value)
        ;; Check if field should be shown
        has-key? (or (:value-fn parameter-map)
                     (contains? (set (keys details)) (:default-key parameter-map)))
        should-show? (and has-key?
                          (:show-to-user? parameter-map)
                          (or (some? raw-value)
                              (:keep? parameter-map)))]
    (if should-show?
      (let [assoc-translation (fn [m]
                                (if (nil? (:translation parameter-map))
                                  (assoc m :translated-value (:raw-value m))
                                  (assoc m :translated-value ((:translation parameter-map) (:raw-value m)))))
            assoc-link (fn [m]
                         (if (nil? (:link-to parameter-map))
                           (assoc m :value (:translated-value m))
                           (assoc m :value
                                  (if (some? ((:link-to parameter-map) details))
                                    [:a {:href ((:link-to parameter-map) details)} (:translated-value m)]
                                    (:translated-value m)))))]
        (as-> {} metadata-map
          (assoc metadata-map
                 :title (:title parameter-map)
                 :help (:help parameter-map)
                 :always-show? (:always-show? parameter-map)
                 :show-in-horizontal? (:show-in-horizontal? parameter-map)
                 :raw-value (if (nil? raw-value)
                              (:display-default parameter-map)
                              raw-value))
          (assoc-translation metadata-map)
          (assoc-link metadata-map)
          (dissoc metadata-map
                  :raw-value
                  :translated-value
                  :translation
                  :link-to)))
      {})))

(defn- ^:no-doc transform-details-to-metadata
  "Transforms all fields of a record for metadata display.

  Applies [[create-structured-param]] to each parameter definition,
  filtering out empty results.

  Arguments:
  - `details` - complete record map from API
  - `parameter-maps` - vector of field metadata definitions

  Returns: vector of structured field maps for metadata-table component."
  [details parameter-maps]
  (let [structured-params (into [] (->> parameter-maps
                                        (map #(create-structured-param % details))
                                        (filter #(not (nil? (:value %))))))]
    (println structured-params)
    structured-params))

;;;; Public API

(defn details->metadata
  "Transforms API response data for metadata-table display.

  Converts raw record details into a structured format suitable for
  the metadata-table component, applying field-specific transformations,
  translations, and link generation based on record type.

  Arguments:
  - `details` - record map as returned from API query
  - `type` - keyword indicating record type: `:title`, `:author`,
             `:chapter`, or `:newspaper`

  Returns: vector of field maps with `:title`, `:help`, `:value`,
           `:always-show?`, `:show-in-horizontal?` keys.

  Example:
  ```clojure
  (details->metadata {:common_title \"The Mystery\" :author_id 5} :title)
  ```"
  [details type]
  (cond (= type :title)
        (transform-details-to-metadata details title-parameters)
        (= type :author)
        (transform-details-to-metadata details author-parameters)
        (= type :chapter)
        (transform-details-to-metadata details chapter-parameters)
        (= type :newspaper)
        (transform-details-to-metadata details newspaper-parameters)
        :else nil))

(defn records->table-data
  "Transforms a collection of records for table display.

  Converts multiple records into a format suitable for titles-table
  or chapters-table components.

  Arguments:
  - `records` - vector of record maps from API
  - `type` - keyword indicating record type: `:chapter` or `:title`

  Returns: transformed table data (currently logs to console).

  TODO: Complete implementation for titles-table and chapters-table components."
  [records type]
  (let [transformed-records
        (cond (= type :chapter)
              (map #(transform-details-to-metadata % chapter-parameters) records)
              (= type :title)
              (map #(transform-details-to-metadata % title-parameters) records)
              :else nil)]
    (println transformed-records)))

(defn new-title-parameters
  "Returns field metadata for the title creation form.

  Filters [[title-parameters]] to include only user-visible fields,
  providing the configuration needed to render the new title form.

  Returns: vector of field metadata maps with `:show-to-user?` true."
  []
  (into [] (->> title-parameters
                (filter #(:show-to-user? %)))))

(defn key->help
  "Looks up help text for a field by key and record type.

  Retrieves the `:help` value from the appropriate *-parameters
  metadata for tooltip or explanatory text display.

  Arguments:
  - `parameter-key` - keyword field name (e.g., `:author_of`)
  - `record-type` - keyword: `:title`, `:author`, `:chapter`, or `:newspaper`

  Returns: help text string, or nil if field not found.

  Example:
  ```clojure
  (key->help :author_of :title)
  ;; => \"Any other works that are attributed to the author...\"
  ```"
  [parameter-key record-type]
  (cond (= record-type :title)
        (get (first (filter #(= (:default-key %) parameter-key) title-parameters)) :help)
        (= record-type :author)
        (get (first (filter #(= (:default-key %) parameter-key) author-parameters)) :help)
        (= record-type :chapter)
        (get (first (filter #(= (:default-key %) parameter-key) chapter-parameters)) :help)
        (= record-type :newspaper)
        (get (first (filter #(= (:default-key %) parameter-key) newspaper-parameters)) :help)
        :else nil))

(defn key->title
  "Looks up display label for a field by key and record type.

  Retrieves the `:title` value from the appropriate *-parameters
  metadata for form labels and table headers.

  Arguments:
  - `parameter-key` - keyword field name (e.g., `:author_of`)
  - `record-type` - keyword: `:title`, `:author`, `:chapter`, or `:newspaper`

  Returns: display label string, or nil if field not found.

  Example:
  ```clojure
  (key->title :author_of :title)
  ;; => \"Attributed Author Of\"
  ```"
  [parameter-key record-type]
  (cond (= record-type :title)
        (get (first (filter #(= (:default-key %) parameter-key) title-parameters)) :title)
        (= record-type :author)
        (get (first (filter #(= (:default-key %) parameter-key) author-parameters)) :title)
        (= record-type :chapter)
        (get (first (filter #(= (:default-key %) parameter-key) chapter-parameters)) :title)
        (= record-type :newspaper)
        (get (first (filter #(= (:default-key %) parameter-key) newspaper-parameters)) :title)
        :else nil))

(defn key->placeholder
  "Looks up placeholder text for a field by key and record type.

  Retrieves the `:placeholder` value from the appropriate *-parameters
  metadata for form input placeholder hints.

  Arguments:
  - `parameter-key` - keyword field name (e.g., `:author_of`)
  - `record-type` - keyword: `:title`, `:author`, `:chapter`, or `:newspaper`

  Returns: placeholder text string, or nil if field not found.

  Example:
  ```clojure
  (key->placeholder :author_of :title)
  ;; => \"'Mr Hogarth's Will', 'Hugh Lindsay's Guest'\"
  ```"
  [parameter-key record-type]
  (cond (= record-type :title)
        (get (first (filter #(= (:default-key %) parameter-key) title-parameters)) :placeholder)
        (= record-type :author)
        (get (first (filter #(= (:default-key %) parameter-key) author-parameters)) :placeholder)
        (= record-type :chapter)
        (get (first (filter #(= (:default-key %) parameter-key) chapter-parameters)) :placeholder)
        (= record-type :newspaper)
        (get (first (filter #(= (:default-key %) parameter-key) newspaper-parameters)) :placeholder)
        :else nil))
