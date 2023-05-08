(ns cde.pages.contribute
  (:require
   [cde.views :refer [contribute-buttons]]))

(defn contribute-page []
  [:section.section>div.container>div.content
   [:h1 "Contribute to the Database"]
   [:p "Have you found a new newspaper, story, or chapter?"]
   [contribute-buttons]])