(ns cde.utils)

(defn length-integer->human-string
"Converts a title 'length' integer to a human-understandable string."
  [length]
  (cond
    (= length 0) "Serialised Title"
    (= length 1) "Short Single Edition"
    (= length 8) "10,000+ Words (Single Edition)"
    :else "Unknown"))


(def ^:private title-parameters
  ;; A map of parameters to expect in a given 'title' response.
  ;; Includes: 'default-key' (the keyword it usually appears as in the response)
  ;;           'show-to-user' (whether you should show it to a frontend user *ever*)
  ;;           'title' (a human-friendly displayable 'title')
  ;;           'keep?' (whether to display if the value is nil)
  ;;           'display-default' (a value to display if the value is nil)
  ;;           'help' (a human-friendly explanation the meaning of the parameter in more detail)
  ;;           'always-show?' (a value to *always* display to users, or a value only shown when the user wants to see it (ie, after clicking for 'more details' or whatever))
  ;;           'translation' (a function for transforming the value for display; usually nil!)
  ;;           'show-in-horizontal? (whether or not to show it in a horizontal table)
  ;;           'link-to' (a function for generating a link to attach to the value; should take the entire title block as an argument; usually nil!)
  [{:default-key :common_title
    :show-to-user? true
    :title "Common Title"
    :keep? true
    :display-default ""
    :help "This is the title that the story is 'commonly known as', even if some newspapers published it under a different title."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to #(str "/#/title/" (:id %))}
   {:default-key :publication_title
    :show-to-user? true
    :title "Publication Title"
    :keep? true
    :display-default ""
    :help "This is the title the story was published under in this particular instance."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to #(str "/#/title/" (:id %))}
   {:default-key :author_common_name
    :show-to-user? true
    :title "Author"
    :keep? true
    :display-default ""
    :help "This is the common name of the author believed to have written the story (not necessarily the name attributed in the publication itself)."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to #(str "#/author/" (:author_id %))}
   {:default-key :span_start
    :show-to-user? true
    :title "Start Date"
    :keep? true
    :display-default ""
    :help "This is the date the first chapter of the story was published."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to nil}
   {:default-key :span_end
    :show-to-user? true
    :title "End Date"
    :keep? true
    :display-default ""
    :help "This is the date the last chapter of the story was published."
    :always-show? true
    :translation nil
    :show-in-horizontal? true
    :link-to nil} 
   {:default-key :length
    :show-to-user? true
    :title "Length"
    :keep? false
    :display-default ""
    :help "This is the length of the title (whether a short story in a single edition, or a story serialised over multiple editions)."
    :always-show? false
    :translation length-integer->human-string
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :inscribed_author_nationality
    :show-to-user? true
    :title "Inscribed Author Nationality"
    :keep? false
    :display-default ""
    :help "This is the nationality of the author as it appears in the publication itself (ie, 'a new story by an Australian author')."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :information_source
    :show-to-user? true
    :title "Information Source"
    :keep? false
    :display-default ""
    :help "This is the source that provides additional information about the title."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :newspaper_common_title
    :show-to-user? true
    :title "Published In"
    :keep? false
    :display-default ""
    :help "This is the 'common' title of the newspaper that published the title was published in."
    :always-show? true
    :translation nil
    :show-in-horizontal? false
    :link-to #(str "/#/newspaper/" (:newspaper_table_id %))}
   {:default-key :newspaper_title
    :show-to-user? true
    :title "Newspaper Title"
    :keep? false
    :display-default ""
    :help "This is the 'long' title of the newspaper that published the title was published in."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to #(str "/#/newspaper/" (:newspaper_table_id %))}
   {:default-key :inscribed_author_gender
    :show-to-user? true
    :title "Inscribed Author Gender"
    :keep? false
    :display-default "None"
    :help "This is the author gender as it appears in the publication itself (ie, 'a new story by a man from Adelaide')."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :attributed_author_name
    :show-to-user? true
    :title "Attributed Author Name"
    :keep? false
    :display-default ""
    :help "This is the name of the author as it appears in the publication itself (ie, 'a new story by John Smith')."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :name_category
    :show-to-user? true
    :title "Publication Attribution Type"
    :keep? false
    :display-default ""
    :help "This is *how* the author is attributed in the publication itself (ie, by name, initials, pseudonym, etc.)"
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :curated_dataset
    :show-to-user? false
    :title "Curated Dataset"
    :keep? false
    :display-default ""
    :help "This is whether the title is part of the curated dataset."
    :always-show? false
    :translation #(if % "Yes" "No")
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :author_of
    :show-to-user? true
    :title "Attributed Author of"
    :keep? false
    :display-default ""
    :help "Any other works that are attributed to the author in the publication itself (ie, 'from the author of A New Othello')."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :also_published
    :show-to-user? true
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
    :title "Trove Source"
    :keep? false
    :display-default ""
    :help "TODO: WORK OUT WHAT THIS ACTUALLY IS!"
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :additional_info
    :show-to-user? true
    :title "Additional Information"
    :keep? false
    :display-default ""
    :help "Any additional information about the title that is not covered by the other fields."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   {:default-key :id
    :show-to-user? false
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
    :title "Author ID"
    :keep? false
    :display-default ""
    :help "This is the unique ID (used in the database) of the author of the title."
    :always-show? false
    :translation nil
    :show-in-horizontal? false
    :link-to nil}
   ])

(def ^:private author-parameters
  ;; TODO: add the author parameters here!
  [])

(def ^:private chapter-parameters
  ;; TODO: add the chapter parameters here!
  [])

(def ^:private newspaper-parameters
  ;; TODO: add the newspaper parameters here!
  [])


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
                         (assoc m :value [:a {:href ((:link-to parameter-map) details)} (:translated-value m)])))]
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
  (if (= type :title)
    (transform-details-to-metadata details title-parameters)
    nil
    )
  )

(defn records->table-data
  "Takes a list of records (typically a vec of chapters or titles)
   and transforms it into a map of table data, suitable for the 'titles-table' or 'chapters-table' components.
   TODO: alter the titles-table and chapters-table components to support this!"
  [records]
  nil)