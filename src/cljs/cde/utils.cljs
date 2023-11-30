(ns cde.utils
  (:require
   [clojure.string :as str]))

;; Helpers for API & endpoint connection
(def api-url "/api/v1") ;; TODO: move to config/env file & switch to versioned api

(defn endpoint
  "Concat params to api-url separated by /"
  [& params]
  (str/join "/" (cons api-url params)))

(defn length-integer->human-string
  "Converts a title 'length' integer to a human-understandable string."
  [length]
  (cond
    (= length 0) "Serialised Title"
    (= length 1) "Short Single Edition"
    (= length 8) "10,000+ Words (Single Edition)"
    :else "Unknown"))

(defn pretty-number
  "Format an integer `n` in a concise, human-readable way for summary display."
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


(def ^:private title-parameters
  ;; A map of parameters to expect in a given 'title' response.
  ;; Includes: 'default-key' (the keyword it usually appears as in the response)
  ;;           'show-to-user?' (whether you should show it to a frontend user *ever*)
  ;;           'editable?' (whether a user can edit the value via '/edit/title/:id')
  ;;           'title' (a human-friendly displayable 'title')
  ;;           'keep?' (whether to display if the value is nil)
  ;;           'display-default' (a value to display if the value is nil)
  ;;           'help' (a human-friendly explanation the meaning of the parameter in more detail)
  ;;           'always-show?' (a value to *always* display to users, or a value only shown when the user wants to see it (ie, after clicking for 'more details' or whatever))
  ;;           'translation' (a function for transforming the value for display; usually nil!)
  ;;           'show-in-horizontal? (whether or not to show it in a horizontal table)
  ;;           'link-to' (a function for generating a link to attach to the value; should take the entire title block as an argument; usually nil!)
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
  ;; A map of parameters to expect in a given 'author' response.
  ;; Includes: 'default-key' (the keyword it usually appears as in the response)
  ;;           'show-to-user?' (whether you should show it to a frontend user *ever*)
  ;;           'title' (a human-friendly displayable 'title')
  ;;           'keep?' (whether to display if the value is nil)
  ;;           'display-default' (a value to display if the value is nil)
  ;;           'help' (a human-friendly explanation the meaning of the parameter in more detail)
  ;;           'always-show?' (a value to *always* display to users, or a value only shown when the user wants to see it (ie, after clicking for 'more details' or whatever))
  ;;           'translation' (a function for transforming the value for display; usually nil!)
  ;;           'show-in-horizontal? (whether or not to show it in a horizontal table)
  ;;           'link-to' (a function for generating a link to attach to the value; should take the entire title block as an argument; usually nil!)
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
  ;; A map of parameters to expect in a given 'chapter' response.
  ;; Includes: 'default-key' (the keyword it usually appears as in the response)
  ;;           'show-to-user?' (whether you should show it to a frontend user *ever*)
  ;;           'title' (a human-friendly displayable 'title')
  ;;           'keep?' (whether to display if the value is nil)
  ;;           'display-default' (a value to display if the value is nil)
  ;;           'help' (a human-friendly explanation the meaning of the parameter in more detail)
  ;;           'always-show?' (a value to *always* display to users, or a value only shown when the user wants to see it (ie, after clicking for 'more details' or whatever))
  ;;           'translation' (a function for transforming the value for display; usually nil!)
  ;;           'show-in-horizontal? (whether or not to show it in a horizontal table)
  ;;           'link-to' (a function for generating a link to attach to the value; should take the entire title block as an argument; usually nil!)
  [{:default-key :chapter_number
    :show-to-user? true
    :title "Chapter Number"
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
    :keep? true
    :display-default "Unnamed Chapter"
    :help "The title of the chapter as it appears in the newspaper."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to #(str "#/chapter/" (:id %))}
   {:default-key :title_common_title
    :show-to-user? true
    :title "Chapter In"
    :keep? true
    :display-default ""
    :help "The title that the story in which the chapter appears is 'commonly known as', even if some newspapers published it under a different title."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to (fn [details]
               (if (:title_id details)
                 (str "/#/title/" (:title_id details))
                 nil))}
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
    :keep? true
    :display-default ""
    :help "The date the chapter was published in the newspaper."
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
    :link-to #(str "https://trove.nla.gov.au/newspaper/article/" %)}
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
  ;; TODO: add the newspaper parameters here!
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


(defn- create-structured-param
  "Take a given 'parameter-map' (ie, one item from the vec of 'title-parameters')
   along with a 'details' map (ie, the details of a given title.
   Return a map of the details *suitable for the 'metadata-table' component*."
  [parameter-map details]
  (if (and (contains? (set (keys details)) (:default-key parameter-map))
           (:show-to-user? parameter-map)
           (or (not (nil? (get details (:default-key parameter-map))))
               (:keep? parameter-map)))
    (let [assoc-translation (fn [m]
                              (if (nil? (:translation parameter-map))
                                (assoc m :translated-value (:raw-value m))
                                (assoc m :translated-value ((:translation parameter-map) (:raw-value m)))))
          assoc-link (fn [m]
                       (if (nil? (:link-to parameter-map))
                         (assoc m :value (:translated-value m))
                         (assoc m :value
                                (if (not (nil? ((:link-to parameter-map) details)))
                                  [:a {:href ((:link-to parameter-map) details)} (:translated-value m)]
                                  (:translated-value m)))))]
      (as-> {} metadata-map
        (assoc metadata-map
               :title (:title parameter-map)
               :help (:help parameter-map)
               :always-show? (:always-show? parameter-map)
               :show-in-horizontal? (:show-in-horizontal? parameter-map)
               :raw-value (if (nil? (get details (:default-key parameter-map)))
                            (:display-default parameter-map)
                            (get details (:default-key parameter-map))))
        (assoc-translation metadata-map)
        (assoc-link metadata-map)
        (dissoc metadata-map
                :raw-value
                :translated-value
                :translation
                :link-to)))
    {}))

(defn- transform-details-to-metadata
  "Take a map of details (of a newspaper, author, chapter, or title record),
   along with a vector of maps describing how to transform those details.
   Returns a map of details that are suitable for the 'metadata-table' component."
  [details parameter-maps]
  (let [structured-params (into [] (->> parameter-maps
                                        (map #(create-structured-param % details))
                                        (filter #(not (nil? (:value %))))))]
    (println structured-params)
    structured-params))

(defn details->metadata
  "Takes a map of details (as returned from a newspaper, author, chapter, or title query)
   along with the 'type' of those details. Uses the 'type' of details to grab a
   'type-parameters' vector of maps (describing how to treat different values in the map)
   and transform the input details so that they're suitable for the 'metadata-table' component."
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
  "Takes a list of records (typically a vec of chapters or titles)
   and the 'type' of those records. Uses the 'type' of details to grab a
   'type-parameters' vector of maps (describing how to treat different values in the map)
   and transforms it into a map of table data, suitable for the 'titles-table' or 'chapters-table' components.
   TODO: alter the titles-table and chapters-table components to support this!"
  [records type]
  (let [transformed-records
        (cond (= type :chapter)
              (map #(transform-details-to-metadata % chapter-parameters) records)
              (= type :title)
              (map #(transform-details-to-metadata % title-parameters) records)
              :else nil)]
    (println transformed-records)))



(defn new-title-parameters
  "Return a list of fields that are used to create a new title"
  []
  (into [] (->> title-parameters
                (filter #(:show-to-user? %)))))



(defn key->help
  "Returns the help text for a given parameter in a
   title, author, chapter, or newspaper record.
   
   Extracts the value of the :help field from, eg, the title-parameters
   for a :parameter-key.
   
   (If the :parameter-key is not found in the title-parameters, returns nil.)
   
   eg: (key->help :author_of :title) => \"Any other works that are attributed to the author in the publication itself (ie, 'from the author of A New Othello').\""
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
  "Returns the title text for a given parameter in a
   title, author, chapter, or newspaper record.
   
   Extracts the value of the :title field from, eg, the title-parameters
   for a :parameter-key.
   
   (If the :parameter-key is not found in the title-parameters, returns nil.)

    eg: (key->title :author_of :title) => \"Attributed Author Of\""
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
  "Return the placeholder text for a given parameter in a
   title, author, chapter, or newspaper.
   
   Useful for displaying placeholder text in a form input.

   Extracts the value of the :placeholder field from, eg, the title-parameters
    for a :parameter-key.
   
    (If the :parameter-key is not found in the title-parameters, returns nil.)
   
    eg: (key->placeholder :author_of :title) => \"eg, 'A New Othello'\""
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