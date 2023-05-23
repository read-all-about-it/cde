(ns cde.components.metadata
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as str]))


(defn metadata-block
  "A table of styled metadata for a newspaper, author, title, or chapter.
   Takes a map of metadata k-v pairs to display. Optionally, takes an ordered
   vector of keys to display, and will display only those keys.
   Optionally also takes pretty-names for the keys, as a map of keyword/string pairs."
  ([metadata]
   [:table.is-hoverable.is-fullwidth
    [:tbody
     (for [[k v] metadata]
       [:tr
        [:th (str/replace (str (name k)) "_" " ")]
        [:td v]])]])
  ([metadata keys-to-display]
   [:table.is-hoverable.is-fullwidth
    [:tbody
     (for [k keys-to-display]
       [:tr
        [:th (str/replace (str (name k)) "_" " ")]
        [:td (get metadata k)]])]])
  ([metadata keys-to-display pretty-names]
   [:table.is-hoverable.is-fullwidth
    [:tbody
     (for [k keys-to-display]
       [:tr
        [:th (get pretty-names k (str/replace (str (name k)) "_" " "))]
        [:td (get metadata k)]])]]))