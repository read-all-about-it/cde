(ns cde.pages.contribute
  (:require [cde.components.nav :refer [page-header]]))

(defn contribute-page []
  [:section.section>div.container>div.content
   [page-header "Contribute to the Database"]
   [:div.block
    [:p "To Be Continued is a community effort."]
    ]
   ])