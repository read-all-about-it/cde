(ns cde.views.search
  "Search page for discovering newspaper fiction records.

  Provides the main search interface for querying titles, authors,
  newspapers, and chapters in the database. Uses the search components
  for input and results display.

  See also: [[cde.components.search]]."
  (:require
   [cde.components.search :refer [search-input search-results]]))

(defn search-page
  "Renders the search page with input field and results.

  Composes the search-input and search-results components to provide
  the full search experience for the Australian Newspaper Fiction Database."
  []
  [:section.section>div.container>div.content
   [:h1
    {:style {:text-align "center"}}
    "Search Australian Newspaper Fiction"]
   [search-input]
   [:br]
   [search-results]])
