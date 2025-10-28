(ns cde.components.search
  "Search interface components for querying the database.

  Provides the search input form with multiple filter options and
  displays results as expandable cards with metadata previews.

  Key components:
  - [[search-input]] - Multi-field search form with filters
  - [[search-results]] - Results display with title/chapter cards

  See also: [[cde.views.search]]."
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [clojure.string :as str]
   [cde.components.metadata :refer [metadata-table]]
   [cde.utils :refer [length-integer->human-string
                      details->metadata]]))

;;;; Private Helpers

(defn- ^:no-doc underline-substring-match
  "Highlights a substring within text by wrapping matches in styled spans.

  Arguments:
  - `text` - full text to search within
  - `substring` - text to highlight
  - `span-style` - optional style map (default: underline)

  Returns: vector of strings and hiccup spans."
  ([text substring span-style]
   (let [split-text (str/split text (re-pattern (str "(?i)(" substring ")")))]
     (into []
           (map #(if (= % substring)
                   [:span {:style span-style} %]
                   %)
                split-text))))
  ([text substring]
   (underline-substring-match text substring {:text-decoration-line "underline"})))

;;;; Filter Components

(defn nationality-options
  "Dropdown select for filtering by author nationality."
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
                  :on-change #(rf/dispatch [:search/update-query :author_nationality (-> % .-target .-value)])}]
                [[:option {:value "" :disabled true :selected true} "Author Nationality"]
                 [:option {:value ""} "Any"]]
                (map (fn [nat] [:option {:value nat} nat])
                     (if @nationalities @nationalities default-nationalities))))
       [:span.icon.is-small.is-left
        [:i.material-icons "public"]]]]]))

(defn gender-options
  "Dropdown select for filtering by author gender."
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
                  :on-change #(rf/dispatch [:search/update-query :author_gender (-> % .-target .-value)])}]
                [[:option {:value "" :disabled true :selected true} "Author Gender"]
                 [:option {:value ""} "Any"]]
                (map (fn [g] [:option {:value g} g])
                     (if @genders @genders default-genders))))
       [:span.icon.is-small.is-left
        [:i.material-icons "transgender"]]]]]))

(defn story-length-options
  "Dropdown select for filtering by story length type."
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
        [:option {:value 8} "Long Single Edition"]]
       [:span.icon.is-small.is-left
        [:i.material-icons "auto_stories"]]]]]))

;;;; Search Input

(defn search-input
  "Main search form with text inputs and filter dropdowns.

  Provides fields for searching by:
  - Title text
  - Newspaper title
  - Author name
  - Nationality, gender, and story length filters

  Submits search on Enter key or Search button click."
  []
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
               :placeholder "Search within the title of a story..."
               :value (:title_text @query)
               :on-key-down #(when (= (.-keyCode %) 13)
                               (do
                                 (rf/dispatch [:search/clear-search-results])
                                 (rf/dispatch [:search/update-query :title_text (-> % .-target .-value)])
                                 (rf/dispatch [:search/submit-titles-search])))
               :on-change #(rf/dispatch [:search/update-query :title_text (-> % .-target .-value)])}]]]
           [:div.field
            [:div.control
             [:input.input
              {:type "text"
               :placeholder "Search by newspaper title..."
               :value (:newspaper_title_text @query)
               :on-key-down #(when (= (.-keyCode %) 13)
                               (do
                                 (rf/dispatch [:search/clear-search-results])
                                 (rf/dispatch [:search/update-query :newspaper_title_text (-> % .-target .-value)])
                                 (rf/dispatch [:search/submit-titles-search])))
               :on-change #(rf/dispatch [:search/update-query :newspaper_title_text (-> % .-target .-value)])}]]]]]
         [:div.field.is-horizontal
          [:div.field-body
           [:div.field
            [:div.control
             [:input.input
              {:type "text"
               :placeholder "Search by author name..."
               :value (:author_name @query)
               :on-key-down #(when (= (.-keyCode %) 13)
                               (do
                                 (rf/dispatch [:search/clear-search-results])
                                 (rf/dispatch [:search/update-query :author_name (-> % .-target .-value)])
                                 (rf/dispatch [:search/submit-titles-search])))
               :on-change #(rf/dispatch [:search/update-query :author_name (-> % .-target .-value)])}]]]
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

(defn- ^:no-doc find-kwic-strings
  "Extracts KWIC (keyword-in-context) snippets from chapter text.

  Finds all occurrences of match-text and returns surrounding context
  (approximately 20 characters on each side).

  Arguments:
  - `raw-chapter-text` - full chapter text
  - `raw-match-text` - search term to find

  Returns: vector of context strings."
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

(defn- ^:no-doc generate-header-from-title-result
  "Generates a card header for a title search result with highlighted matches."
  [result query]
  (let [result-author-name (if (not (empty? (get result :attributed_author_name "")))
                             (:attributed_author_name result)
                             (:author_common_name result))
        display-author (if (not (empty? (get query :author_name "")))
                         (underline-substring-match
                          result-author-name
                          (:author_name query))
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
        display-title (if (not (empty? (get query :title_text "")))
                        (underline-substring-match
                         result-title
                         (:title_text query))
                        [result-title])
        result-newspaper (get result :newspaper_title "")
        raw-newspaper (if (not (empty? (get query :newspaper_title_text "")))
                        (underline-substring-match
                         result-newspaper
                         (:newspaper_title_text query))
                        [result-newspaper])
        display-newspaper [(apply vector (concat [:span {:style {:font-style "italic"}}] raw-newspaper))]]
    (apply vector (cons :p (into [] (concat display-title [" by "] display-author [" — as published in "] display-newspaper))))))

(defn- ^:no-doc generate-header-for-chapter-result
  "Generates a card header for a chapter search result with KWIC snippet."
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
                                   (if (not (empty? (:newspaper_title result)))
                                     [(str " — " (:newspaper_title result))]
                                     []))))))

;;;; Result Cards

(defn search-result-card
  "Expandable card component for displaying a single search result.

  Arguments:
  - `card-header` - hiccup for the clickable header
  - `card-content` - expanded content (typically metadata-table)
  - `card-footer-items` - footer action links"
  [card-header card-content card-footer-items]
  (let [is-collapsed? (r/atom true)]
    (fn []
      [:div.card.is-collapsible.is-active
       [:header.card-header
        {:on-click #(reset! is-collapsed? (not @is-collapsed?))}
        [:p.card-header-title.is-centered
         [:span
          card-header]]
        (if @is-collapsed?
          [:button.card-header-icon {:aria-label "more-options"}
           [:span.icon
            [:i.material-icons {:aria-hidden "true"} "keyboard_arrow_down"]]]
          [:button.card-header-icon
           [:span.icon
            [:i.material-icons "keyboard_arrow_up"]]])]
       [:div.card-content
        {:style {:display (if @is-collapsed? "none" "block")}}
        [:div.content
         card-content]]
       (into [] (concat [:footer.card-footer (when @is-collapsed? {:style {:display "none"}})]
                        card-footer-items))])))

(defn title-search-results
  "Renders search result cards for title queries."
  []
  (r/with-let [results (rf/subscribe [:search/results])
               query (rf/subscribe [:search/query])
               logged-in? (rf/subscribe [:auth/logged-in?])]
    [:div
     (for [result @results]
       (let [metadata (into [] (filter #(:always-show? %)
                                       (details->metadata
                                        (dissoc result :newspaper_title) ;; don't show newspaper common title in metadata block
                                        :title)))
             header (generate-header-from-title-result result @query)]
         [:div
          [search-result-card
           header
           [metadata-table metadata]
           [[:a.card-footer-item {:href (str "#/title/" (:id result))}
             [:span "View Title"]]
            (when @logged-in?
              [:a.card-footer-item {:href (str "#/edit/title/" (:id result))}
               [:span "Correct Metadata"]]
              ;; [:a.card-footer-item {:href "#"}
              ;;  [:span "Add to Bookmarks"]]
              )]]
          [:br]]))]))

(defn chapter-search-results
  "Renders search result cards for chapter text queries with KWIC snippets."
  []
  (r/with-let [results (rf/subscribe [:search/results])
               query (rf/subscribe [:search/query])
               logged-in? (rf/subscribe [:auth/logged-in?])]
    [:div
     (for [result @results]
       (let [metadata (into [] (filter #(:always-show? %) (details->metadata result :chapter)))
             query-match-text (get @query :chapter_text "")]
         (for [kwic-string (find-kwic-strings (get result :chapter_text "") query-match-text)]
           (let [header (generate-header-for-chapter-result result kwic-string query-match-text)]
             [:div
              [search-result-card
               header
               [metadata-table metadata]
               [[:a.card-footer-item {:href (str "#/chapter/" (:id result))}
                 [:span "View Chapter"]]
                (when @logged-in? [:a.card-footer-item {:href "#"}
                                   [:span "Correct Metadata"]])
                ;; (when @logged-in? [:a.card-footer-item {:href "#"}
                                  ;;  [:span "Add to Bookmarks"]])
                ]]
              [:br]]))))]))

;;;; Main Results Component

(defn search-results
  "Main container for search results with loading and empty states.

  Displays appropriate results component based on search type
  (title vs chapter), handles loading indicators, and shows
  'no results' message when applicable."
  []
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
