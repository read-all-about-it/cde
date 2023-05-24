(ns cde.components.search
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [clojure.string :as str]
   [cde.components.metadata :refer [metadata-table]]))


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


(defn- convert-length-int-to-string
  "Converts a length integer to a string"
  [length]
  (cond
    (= length 0) "Serialised Title"
    (= length 1) "Short Single Edition"
    (= length 8) "10,000+ Words (Single Edition)"
    :else "Unknown"))


(defn search-input []
  (r/with-let [query (rf/subscribe [:search/query])
               search-chapters? (r/atom false) ;; search at the 'titles'/'stories' level by default
               error (r/atom nil)]
    (fn []
      [:div
       [:div
        [:div.field
         {:style {:text-align "center"}}
         [:input.switch.is-rtl.is-rounded
          {:id "chapter-switch"
           :type "checkbox"
           :name "chapter-switch"
           :checked @search-chapters?
           :on-change #(do (reset! search-chapters? (not @search-chapters?))
                           (rf/dispatch [:search/update-query :search-chapters (not @search-chapters?)]))}]
         [:label
          {:for "chapter-switch"}
          "Search Chapters?"]]
        [:div
         [:div.field.is-horizontal
          (if-not @search-chapters?
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input
                {:type "text"
                 :placeholder "Search by common title..."
                 :value (:common-title @query)
                 :on-change #(rf/dispatch [:search/update-query :common-title (-> % .-target .-value)])}]]]
             [:div.field
              [:div.control
               [:input.input
                {:type "text"
                 :placeholder "Search by newspaper title..."
                 :value (:newspaper-title @query)
                 :on-change #(rf/dispatch [:search/update-query :newspaper-title (-> % .-target .-value)])}]]]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input
                {:type "text"
                 :placeholder "Search for chapter text..."
                 :value (:common-title @query)
                 :on-change #(rf/dispatch [:search/update-query :chapter-text (-> % .-target .-value)])}]]]])]
         [:div.field.is-horizontal
          [:div.field-body
           [:div.field
            [:div.control
             [:input.input
              {:type "text"
               :placeholder "Search by author name..."
               :value (:author @query)
               :on-change #(rf/dispatch [:search/update-query :author (-> % .-target .-value)])}]]]
           [:div.field
            [:div.control.has-icons-left
             [:div.select
              [:select
               {:value (:nationality @query)
                :on-change #(rf/dispatch [:search/update-query :nationality (-> % .-target .-value)])}
               [:option {:value "" :disabled true :selected true} "Author Nationality"]
             ; Add more options for each nationality
               [:option {:value ""} "Any"]
               [:option {:value "Australian"} "Australian"]
               [:option {:value "British"} "British"]]
              [:span.icon.is-small.is-left
               [:i.material-icons "public"]]]]]
           [:div.field
            [:div.control.has-icons-left
             [:div.select
              [:select
               {:value (:gender @query)
                :on-change #(rf/dispatch [:search/update-query :gender (-> % .-target .-value)])}
               [:option {:value "" :disabled true :selected true} "Author Gender"]
             ; Add more options for each gender
               [:option {:value ""} "Any"]
               [:option {:value "Female"} "Female"]
               [:option {:value "Male"} "Male"]
               [:option {:value "Multiple"} "Multiple"]
               [:option {:value "Unknown"} "Unknown"]]
              [:span.icon.is-small.is-left
               [:i.material-icons "transgender"]]]]]
           (when-not @search-chapters?
             [:div.field
              [:div.control.has-icons-left
               [:div.select
                [:select
                 {:value (:length @query)
                  :on-change #(rf/dispatch [:search/update-query :length (-> % .-target .-value)])}
                 [:option {:value "" :disabled true :selected true} "Story Length"]
             ;; TODO: Fix these options
                 [:option {:value ""} "Any"]
                 [:option {:value 0} "Serialised Title"]
                 [:option {:value 1} "Short Single Edition"]
                 [:option {:value 8} "10,000+ Words (Single Edition)"]]
                [:span.icon.is-small.is-left
                 [:i.material-icons "auto_stories"]]]]])]]]]
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
            {:on-click #(do
                          (rf/dispatch [:search/clear-search-results])
                          (rf/dispatch [:search/submit-search]))}
            "Search"]]]]]])))


(defn- convert-title-search-result-to-metadata
  "Takes a search result map and converts it to a vector of maps suitable for the
   'metadata-table' component. Optionally takes a 'searh-query' parameter, which
   is used to ensure that the metadata table includes extra fields if those fields
   were used in the search."
  ([result]
   (convert-title-search-result-to-metadata result {}))
  ([result search-query]
   (let [structured-results
         [{:title "Publication Title"
           :value (:publication_title result)
           :link (str "#/title/" (:id result))}
          {:title "Common Title"
           :value (:common_title result)
           :link (str "#/title/" (:id result))}
          {:title "Published In"
           :value (:newspaper_title result)
           :link (str "#/newspaper/" (:newspaper_table_id result))}
          {:title "Start Date"
           :value (:span_start result)}
          {:title "End Date"
           :value (:span_end result)}
          {:title "Author"
           :value (:author_common_name result)
           :link (str "#/author/" (:author_id result))}
          (if (and (:author search-query)
                   (not (str/includes? (:author_common_name result) (:author search-query))))
            {:title "Author Other Names"
             :value (apply vector (cons :span (underline-substring-match (:author_other_name result)
                                                                         (:author search-query))))}
            {})
          (if (:gender search-query)
            {:title "Author Gender"
             :value (apply vector (cons :span (underline-substring-match (:author_gender result)
                                                                         (:gender search-query))))}
            {})
          (if (:nationality search-query)
            {:title "Author Nationality" 
             :value (apply vector (cons :span
                                        (underline-substring-match (:author_nationality result)
                                                                   (:nationality search-query))))}
            {})
          ;;(if (:length search-query)
          ;;  {:title "Length"
          ;;   :value [:span {:style {:text-decoration-line "underline"}}
           ;;          (convert-length-int-to-string (:length result))]}
          ;;  {:title "Length"
          ;;   :value (convert-length-int-to-string (:length result))})
          
          ]]
    ; remove all results where the value is nil
     (filter #(not (nil? (:value %))) structured-results))))



(defn- generate-header-from-result
  "Takes a search result map (and search query) and returns a vector of
   sometimes-span-underlined strings, suitable for the 'header' component of a card."
  [result query]
  (let [title (if-not (empty? (get query :common-title ""))
                (underline-substring-match (:common_title result) (:common-title query)) 
                [(:common_title result)])
        author (if-not (empty? (get query :author ""))
                 (underline-substring-match (:author_common_name result) (:author query)) 
                 [(:author_common_name result)])
        newspaper (if-not (empty? (get query :newspaper-title ""))
                    (underline-substring-match (:newspaper_title result) (:newspaper-title query)) 
                    [(:newspaper_title result)])
        ]
    (apply vector (cons :p (into [] (concat title [" — "] author [" — "] newspaper))))))

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
         [:button.card-header-icon
          [:span.icon
           [:i.material-icons "keyboard_arrow_down"]]]]]
       [:div.card-content
        {:style {:display (if @is-collapsed? "none" "block")}}
        [:div.content
         card-content]]
       (into [] (concat [:footer.card-footer (when @is-collapsed? {:style {:display "none"}})]
                        card-footer-items))])))

(defn search-results []
  (r/with-let [results (rf/subscribe [:search/results])
               loading? (rf/subscribe [:search/loading?])
               query (rf/subscribe [:search/query])
               logged-in? (rf/subscribe [:auth/logged-in?])
               time-loaded (rf/subscribe [:search/time-loaded])
               error (r/atom nil)]
    (fn []
      [:div
       (when-not (str/blank? @error)
         [:div.notification.is-danger
          @error])
       (for [result @results]
         (let [metadata (convert-title-search-result-to-metadata result @query)
               header (generate-header-from-result result @query)]
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
            [:br]]))
       (when @loading?
         ;; show a nice bulma indeterminate progress bar
         [:progress.progress.is-small.is-primary
          {:max "100"}])])))