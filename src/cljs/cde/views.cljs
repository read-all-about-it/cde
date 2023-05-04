(ns cde.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.subs]
   [cde.events]))

(defn search-results []
  (let [results (rf/subscribe [:search/results])
        loading (rf/subscribe [:search/loading?])]
    (fn []
      ;; show whatever results are present
      (if @loading
        [:div "Loading..."]
        [:div "Results!"]))))