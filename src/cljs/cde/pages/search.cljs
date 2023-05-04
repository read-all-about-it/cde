(ns cde.pages.search
  (:require
   [cde.components.forms :refer [search-input]]
   [cde.views :refer [search-results]]))

(defn search-page []
  [:section.section>div.container>div.content
   [:h1 "Search Australian Newspaper Fiction"]
   [search-input]])