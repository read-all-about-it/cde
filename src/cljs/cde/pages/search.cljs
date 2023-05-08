(ns cde.pages.search
  (:require
   [cde.components.search :refer [search-input search-results]]))

(defn search-page []
  [:section.section>div.container>div.content
   [:h1 
    {:style {:text-align "center"}}
     "Search Australian Newspaper Fiction"]
   [search-input]
   [:br]
   [search-results]])