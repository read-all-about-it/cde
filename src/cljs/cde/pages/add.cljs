(ns cde.pages.add
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]))


(defn add-newspaper-page []
  (fn []
    [:section.section>div.container>div.content
     [:h1 "Add Newspaper"]
     ;; 
     ]))