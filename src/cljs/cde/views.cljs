(ns cde.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.subs]
   [cde.events]
   [clojure.string :as string]))

(defn contribute-buttons
  "Buttons for contributing new (found) newspapers, authors, chapters, and stories." 
  []
  (fn []
    [:div.container {:style {:text-align "center"}}
     [:div.buttons
      [:a.button.is-primary
       {:href "#/contribute/newspaper"}
       "Newspaper"]
      [:a.button.is-primary
       {:href "#/contribute/author"}
       "Author"]
      [:a.button.is-primary
       {:href "#/contribute/chapter"}
       "Chapter"]
      [:a.button.is-primary
       {:href "#/contribute/story"}
       "Story"]]]))