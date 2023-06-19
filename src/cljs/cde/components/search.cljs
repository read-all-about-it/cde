(ns cde.components.search
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [clojure.string :as str]
   [cde.components.metadata :refer [metadata-table]]
   [cde.utils :refer [length-integer->human-string
                      details->metadata]]))


(defn- underline-substring-match
  "Takes a text string and a substring, and returns a vector of strings
  where the substring match is highlighted.
   eg 'The quick brown fox' 'quick' becomes
   ['The ' [:span {:style {:text-decoration 'underline'}} 'quick'] ' brown fox']
   "
  ([text substring span-style]
   (let [split-text (str/split text (re-pattern (str "(?i)(" substring ")")))]
     (into []
           (map #(if (= % substring)
                   [:span {:style span-style} %]
                   %)
                split-text))))
  ([text substring]
   (underline-substring-match text substring {:text-decoration-line "underline"})))

(defn nationality-options
  "A component for setting the 'author nationality' option in a search block."
  []
  (r/with-let [nationalities (rf/subscribe [:platform/author-nationalities])
               default-nationalities ["British" "Australian"]
               query (rf/subscribe [:search/query])]
    [:div.field
     [:div.control.has-icons-left
      [:div.select
       (apply
        vector
        (concat [:select]
                [{:value (:nationality @query)
                  :on-change #(rf/dispatch [:search/update-query :nationality (-> % .-target .-value)])}]
                [[:option {:value "" :disabled true :selected true} "Author Nationality"]
                 [:option {:value ""} "Any"]]
                (map (fn [nat] [:option {:value nat} nat])
                     (if @nationalities @nationalities default-nationalities))))
       [:span.icon.is-small.is-left
        [:i.material-icons "public"]]]]]))

(defn gender-options
  "A component for setting the 'author gender' option in a search block."
  []
  (r/with-let [genders (rf/subscribe [:platform/author-genders])
               default-genders ["Male" "Female" "Unknown"]
               query (rf/subscribe [:search/query])]
    [:div.field
     [:div.control.has-icons-left
      [:div.select
       (apply
        vector
        (concat [:select]
                [{:value (:gender @query)
                  :on-change #(rf/dispatch [:search/update-query :gender (-> % .-target .-value)])}]
                [[:option {:value "" :disabled true :selected true} "Author Gender"]
                 [:option {:value ""} "Any"]]
                (map (fn [g] [:option {:value g} g])
                     (if @genders @genders default-genders))))
       [:span.icon.is-small.is-left
        [:i.material-icons "transgender"]]]]]))

(defn story-length-options
  "A component for setting the 'story length' option in a search block."
  []
  (r/with-let [query (rf/subscribe [:search/query])]
    [:div.field
     [:div.control.has-icons-left
      [:div.select
       [:select
        {:value (:length @query)
         :on-change #(rf/dispatch [:search/update-query :length (-> % .-target .-value)])}
        [:option {:value "" :disabled true :selected true} "Story Length"]
        [:option {:value ""} "Any"]
        [:option {:value 0} "Serialised Title"]
        [:option {:value 1} "Short Single Edition"]
        [:option {:value 8} "10,000+ Words (Single Edition)"]]
       [:span.icon.is-small.is-left
        [:i.material-icons "auto_stories"]]]]]))

(defn search-input []
  (r/with-let [query (rf/subscribe [:search/query])
               error (r/atom nil)]
    (fn []
      [:div
       [:div
        [:div
         [:div.field.is-horizontal
          [:div.field-body
           [:div.field
            [:div.control
             [:input.input
              {:type "text"
               :placeholder "Search for text within a chapter..."
               :value (:chapter-text @query)
               :on-change #(rf/dispatch [:search/update-query :chapter-text (-> % .-target .-value)])
               :on-key-down #(when (= (.-keyCode %) 13)
                               (do
                                 (rf/dispatch [:search/clear-search-results])
                                 (rf/dispatch [:search/update-query :chapter-text (-> % .-target .-value)])
                                 (rf/dispatch [:search/submit-chapter-text-search])))}]]]]]
         [:div.field.is-horizontal
          [:div.field-body
           [:div.field
            [:div.control
             [:input.input
              {:type "text"
               :placeholder "Search within the title of a story..."
               :value (:common-title @query)
               :on-key-down #(when (= (.-keyCode %) 13)
                               (do
                                 (rf/dispatch [:search/clear-search-results])
                                 (rf/dispatch [:search/update-query :common-title (-> % .-target .-value)])
                                 (rf/dispatch [:search/submit-titles-search])))
               :on-change #(rf/dispatch [:search/update-query :common-title (-> % .-target .-value)])}]]]
           [:div.field
            [:div.control
             [:input.input
              {:type "text"
               :placeholder "Search by newspaper title..."
               :value (:newspaper-title @query)
               :on-key-down #(when (= (.-keyCode %) 13)
                               (do
                                 (rf/dispatch [:search/clear-search-results])
                                 (rf/dispatch [:search/update-query :newspaper-title (-> % .-target .-value)])
                                 (rf/dispatch [:search/submit-titles-search])))
               :on-change #(rf/dispatch [:search/update-query :newspaper-title (-> % .-target .-value)])}]]]]]
         [:div.field.is-horizontal
          [:div.field-body
           [:div.field
            [:div.control
             [:input.input
              {:type "text"
               :placeholder "Search by author name..."
               :value (:author @query)
               :on-key-down #(when (= (.-keyCode %) 13)
                               (do
                                 (rf/dispatch [:search/clear-search-results])
                                 (rf/dispatch [:search/update-query :author (-> % .-target .-value)])
                                 (rf/dispatch [:search/submit-titles-search])))
               :on-change #(rf/dispatch [:search/update-query :author (-> % .-target .-value)])}]]]
           [nationality-options]
           [gender-options]
           [story-length-options]]]]]
       ;; the 'search' button, which (on click) will dispatch the submit-search
       ;; event (allowing the 'search-result' component placed below this on
       ;; on the search page to start populating results as they come in from
       ;; the api
       [:br]
       [:div.field.is-horizontal
        {:style {:text-align "center"}}
        [:div.field-body.is-expanded
         [:div.field
          [:div.control
           [:button.button.is-primary
            {:on-click #(if (empty? (:chapter-text @query))
                          (do
                            (rf/dispatch [:search/clear-search-results])
                            (rf/dispatch [:search/submit-titles-search]))
                          (do
                            (rf/dispatch [:search/clear-search-results])
                            (rf/dispatch [:search/submit-chapter-text-search])))}
            "Search"]]]]]])))




(defn- find-kwic-strings
  "Given a chapter text string and a match-text string, return a vector of
   strings that are the match-text surrounded by 5 characters either side.
   Gets the indices of the match-text in the chapter text, then returns a
    vector of strings that are the match-text surrounded by 5 characters
    either side."
  [raw-chapter-text raw-match-text]
  (let [chapter-text (str/lower-case (str/replace raw-chapter-text "\n" " "))
        match-text (str/lower-case raw-match-text)
        ;; a regex pattern that matches the match-text with 0-20 characters either side
        match-pattern (str "(.{0,20})" match-text "(.{0,20})")
        raw-matches (re-seq (re-pattern match-pattern) chapter-text)
        ;; for each match, slice to the space either side (effectively removing any partial words)
        matches (map #(str/split (first %) #" ") raw-matches)
        matches (map #(str/join " " (subvec % 1 (dec (count %)))) matches)]
    (into [] matches)))


(defn- generate-header-from-title-result
  "Takes a search result map (and search query) and returns a vector of
   sometimes-span-underlined strings, suitable for the 'header' component of a card."
  [result query]
  (let [result-author-name (if (not (empty? (get result :attributed_author_name "")))
                             (:attributed_author_name result)
                             (:author_common_name result))
        display-author (if (not (empty? (get query :author "")))
                         (underline-substring-match
                          result-author-name
                          (:author query))
                         [result-author-name])
        raw-result-title (if (not (empty? (get result :publication_title "")))
                           (:publication_title result)
                           (:common_title result))
        result-title (-> raw-result-title
                         (str/replace "\"" "")
                         (str/replace "“" "")
                         (str/replace "”" "")
                         ;; prepend "“" to the start of the title
                         (str/replace (re-pattern "^") "“")
                          ;; append "”" to the end of the title
                         (str/replace (re-pattern "$") "”"))
        display-title (if (not (empty? (get query :common-title "")))
                        (underline-substring-match
                         result-title
                         (:common-title query))
                        [result-title])
        result-newspaper (if (not (empty? (get result :newspaper_common_title "")))
                           (:newspaper_common_title result)
                           (:newspaper_title result))
        raw-newspaper (if (not (empty? (get query :newspaper-title "")))
                        (underline-substring-match
                         result-newspaper
                         (:newspaper-title query))
                        [result-newspaper])
        display-newspaper [(apply vector (concat [:span {:style {:font-style "italic"}}] raw-newspaper))]]
    (apply vector (cons :p (into [] (concat display-title [" by "] display-author [" — as published in "] display-newspaper))))))


(defn- generate-header-for-chapter-result
  "Generate a card header for a given chapter search result (and specific substring match)."
  [result match-kwic-string query-text]
  (apply vector (cons :p (into [] (concat
                                   ["\""]
                                   (underline-substring-match match-kwic-string (str/lower-case query-text))
                                   ["\""]
                                   (if (not (empty? (:chapter_title result)))
                                     [(str " — " (:chapter_title result))]
                                     [])
                                   (if (not (empty? (:author_common_name result)))
                                     [(str " — " (:author_common_name result))]
                                     [])
                                    (if (not (empty? (:newspaper_common_title result)))
                                      [(str " — " (:newspaper_common_title result))]
                                      []))))))

(defn search-result-card
  "A single search result card"
  [card-header card-content card-footer-items]
  (let [is-collapsed? (r/atom true)]
    (fn []
      [:div.card.is-collapsible.is-active
       [:header.card-header
        {:on-click #(reset! is-collapsed? (not @is-collapsed?))}
        [:p.card-header-title
         [:span
          card-header]
         (if @is-collapsed?
           [:button.card-header-icon
            [:span.icon
             [:i.material-icons "keyboard_arrow_down"]]]
           [:button.card-header-icon
            [:span.icon
             [:i.material-icons "keyboard_arrow_up"]]])]]
       [:div.card-content
        {:style {:display (if @is-collapsed? "none" "block")}}
        [:div.content
         card-content]]
       (into [] (concat [:footer.card-footer (when @is-collapsed? {:style {:display "none"}})]
                        card-footer-items))])))


(defn title-search-results
  "A div of cards for each result from a title search."
  []
  (r/with-let [results (rf/subscribe [:search/results])
               query (rf/subscribe [:search/query])
               logged-in? (rf/subscribe [:auth/logged-in?])]
    [:div
     (for [result @results]
       (let [metadata (into [] (filter #(:always-show? %)
                                       (details->metadata
                                        (dissoc result :newspaper_common_title) ;; don't show newspaper common title in metadata block
                                        :title)))
             header (generate-header-from-title-result result @query)]
         [:div
          [search-result-card
           header
           [metadata-table metadata]
           [[:a.card-footer-item {:href (str "#/title/" (:id result))}
             [:span "View Title"]]
            [:a.card-footer-item {:href "#"}
             [:span "Correct Metadata"]]
            (when @logged-in? [:a.card-footer-item {:href "#"}
                               [:span "Add to Bookmarks"]])]]
          [:br]]))]))

(defn chapter-search-results
  "A div of cards for each result from a title search."
  []
  (r/with-let [results (rf/subscribe [:search/results])
               query (rf/subscribe [:search/query])
               logged-in? (rf/subscribe [:auth/logged-in?])]
    [:div
     (for [result @results]
       (let [metadata (into [] (filter #(:always-show? %) (details->metadata result :chapter)))
             query-match-text (get @query :chapter-text "")]
         (for [kwic-string (find-kwic-strings (get result :chapter_text "") query-match-text)]
           (let [header (generate-header-for-chapter-result result kwic-string query-match-text)]
             [:div
              [search-result-card
               header
               [metadata-table metadata]
               [[:a.card-footer-item {:href (str "#/chapter/" (:id result))}
                 [:span "View Chapter"]]
                [:a.card-footer-item {:href "#"}
                 [:span "Correct Metadata"]]
                (when @logged-in? [:a.card-footer-item {:href "#"}
                                   [:span "Add to Bookmarks"]])]]
              [:br]]))))]))


(defn search-results []
  (r/with-let [results (rf/subscribe [:search/results])
               loading? (rf/subscribe [:search/loading?])
               query (rf/subscribe [:search/query])
               logged-in? (rf/subscribe [:auth/logged-in?])
               time-loaded (rf/subscribe [:search/time-loaded])
               error (r/atom nil)
               search-type (rf/subscribe [:search/type])]
    (fn []
      [:div
       (when-not (str/blank? @error)
         [:div.notification.is-danger
          @error])
       (when (and (not @loading?)
                  (empty? @results)
                  (not-empty @query)
                  (not (nil? @time-loaded)))
         [:div.notification.is-warning
          "No results found"])
       (cond (= @search-type "title")
             [title-search-results]
             (= @search-type "chapter")
             [chapter-search-results]
             :else [:div])
       (when @loading?
         ;; show a nice bulma indeterminate progress bar
         [:progress.progress.is-small.is-primary
          {:max "100"}])])))