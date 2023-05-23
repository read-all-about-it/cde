(ns cde.components.metadata
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]))


(defn metadata-block
  "A table of styled metadata for a newspaper, author, title, or chapter.
   Takes a map of metadata k-v pairs to display. Optionally, takes an ordered
   vector of keys to display."
  ([metadata]
   [:table.is-hoverable.is-fullwidth
    [:tbody
     (for [[k v] metadata]
       [:tr
        [:th (str k)]
        [:td v]])]])
  ([metadata keys]
   [:table.is-hoverable.is-fullwidth
    [:tbody
     (for [k keys]
       [:tr
        [:th (str k)]
        [:td (get metadata k)]])]]))